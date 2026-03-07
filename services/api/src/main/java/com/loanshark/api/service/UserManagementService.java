package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.ResetPasswordRequest;
import com.loanshark.api.dto.ApiDtos.UserRequest;
import com.loanshark.api.dto.ApiDtos.UserResponse;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public UserManagementService(
        UserRepository userRepository,
        BorrowerRepository borrowerRepository,
        PasswordEncoder passwordEncoder,
        CurrentUserService currentUserService,
        AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.borrowerRepository = borrowerRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String query, int page, int size) {
        Page<User> userPage = userRepository.search(
            query == null ? "" : query.trim(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PageResponse<>(
            userPage.getContent().stream().map(this::toResponse).toList(),
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages()
        );
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required when creating a user");
        }
        ensureUsernameAvailable(request.username(), null);

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(request.status());
        user = userRepository.save(user);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "CREATE_USER", "User", user.getId().toString(), user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        ensureUsernameAvailable(request.username(), user.getId());
        protectLastOwner(user, request.role());

        user.setUsername(request.username());
        user.setRole(request.role());
        user.setStatus(request.status());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user = userRepository.save(user);
        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPDATE_USER", "User", user.getId().toString(), user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can reset user passwords");
        }
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.record(currentUser.getId(), "RESET_PASSWORD", "User", user.getId().toString(), user.getUsername());
    }

    @Transactional
    public void deleteUser(UUID id) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new ResponseStatusException(FORBIDDEN, "You cannot delete your own account");
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (user.getRole() == UserRole.OWNER && userRepository.countByRole(UserRole.OWNER) <= 1) {
            throw new ResponseStatusException(FORBIDDEN, "At least one owner account must remain");
        }

        borrowerRepository.findByUserId(user.getId()).ifPresent(borrower -> detachBorrowerUser(borrower));
        auditLogService.record(currentUser.getId(), "DELETE_USER", "User", user.getId().toString(), user.getUsername());
        userRepository.delete(user);
    }

    private void detachBorrowerUser(Borrower borrower) {
        borrower.setUser(null);
        borrowerRepository.save(borrower);
    }

    private void ensureUsernameAvailable(String username, UUID userId) {
        userRepository.findByUsername(username)
            .filter(existing -> !existing.getId().equals(userId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
            });
    }

    private void protectLastOwner(User existingUser, UserRole requestedRole) {
        if (existingUser.getRole() == UserRole.OWNER
            && requestedRole != UserRole.OWNER
            && userRepository.countByRole(UserRole.OWNER) <= 1) {
            throw new ResponseStatusException(FORBIDDEN, "At least one owner account must remain");
        }
    }

    private UserResponse toResponse(User user) {
        UUID borrowerId = borrowerRepository.findByUserId(user.getId())
            .map(Borrower::getId)
            .orElse(null);

        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            borrowerId
        );
    }
}
