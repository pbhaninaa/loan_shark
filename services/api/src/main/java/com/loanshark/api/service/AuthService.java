package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.AuthMeResponse;
import com.loanshark.api.dto.ApiDtos.AuthRequest;
import com.loanshark.api.dto.ApiDtos.AuthResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerRegistrationRequest;
import com.loanshark.api.dto.ApiDtos.ChangePasswordRequest;
import com.loanshark.api.dto.ApiDtos.ForgotPasswordRequest;
import com.loanshark.api.dto.ApiDtos.ForgotPasswordResponse;
import com.loanshark.api.dto.ApiDtos.OwnerRegistrationRequest;
import com.loanshark.api.dto.ResetPasswordWithTokenRequest;
import com.loanshark.api.dto.ApiDtos.SetupStatusResponse;
import com.loanshark.api.dto.ApiDtos.StaffRegistrationRequest;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.PasswordResetToken;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.entity.UserStatus;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.PasswordResetTokenRepository;
import com.loanshark.api.repository.UserRepository;
import com.loanshark.api.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.password-reset.base-url:http://localhost:5174}")
    private String passwordResetBaseUrl;

    @Value("${app.password-reset.token-valid-hours:24}")
    private int tokenValidHours;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            BorrowerRepository borrowerRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUserService currentUserService,
            EmailNotificationService emailNotificationService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.borrowerRepository = borrowerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
        this.emailNotificationService = emailNotificationService;
    }

    public SetupStatusResponse setupStatus() {
        return new SetupStatusResponse(userRepository.existsByRole(UserRole.OWNER));
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid credentials"));

        UUID borrowerId = borrowerRepository.findByUserId(user.getId())
                .map(Borrower::getId)
                .orElse(null);

        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getUsername(), user.getRole(), borrowerId);
    }

    /** Current user info for account page; email is used for password reset. */
    @Transactional(readOnly = true)
    public AuthMeResponse getMe() {
        User user = currentUserService.requireCurrentUser();
        String email = null;
        UUID borrowerId = null;
        if (user.getRole() == UserRole.BORROWER) {
            Borrower b = borrowerRepository.findByUserId(user.getId()).orElse(null);
            if (b != null) {
                email = b.getEmail();
                borrowerId = b.getId();
            }
        } else {
            email = user.getEmail();
        }
        if (email != null && email.isBlank()) email = null;
        return new AuthMeResponse(user.getId(), user.getUsername(), user.getRole(), email, borrowerId);
    }

    /** Update current user's email (for password reset). Borrowers update borrowers.email; staff update users.email. */
    @Transactional
    public void updateMyEmail(String email) {
        User user = currentUserService.requireCurrentUser();
        String trimmed = email != null ? email.trim() : "";
        if (user.getRole() == UserRole.BORROWER) {
            Borrower b = borrowerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
            b.setEmail(trimmed.isEmpty() ? null : trimmed);
            borrowerRepository.save(b);
        } else {
            user.setEmail(trimmed.isEmpty() ? null : trimmed);
            userRepository.save(user);
        }
    }

    @Transactional
    public AuthResponse registerOwner(OwnerRegistrationRequest request) {
        if (userRepository.existsByRole(UserRole.OWNER)) {
            throw new ResponseStatusException(FORBIDDEN, "Owner account already exists");
        }
        User user = createUser(request.username(), request.password(), UserRole.OWNER);
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getUsername(), user.getRole(), null);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse registerStaff(StaffRegistrationRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can register staff users");
        }
        if (request.role() != UserRole.OWNER && request.role() != UserRole.CASHIER) {
            throw new ResponseStatusException(BAD_REQUEST, "Staff role must be OWNER or CASHIER");
        }

        User user = createUser(request.username(), request.password(), request.role());
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getUsername(), user.getRole(), null);
    }

    @Transactional
    public AuthResponse registerBorrower(BorrowerRegistrationRequest request) {
        throw new ResponseStatusException(BAD_REQUEST, "Borrower registration now requires KYC document upload");
    }

    User createUser(String username, String password, UserRole role) {
        ensureUsernameAvailable(username);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    void ensureUsernameAvailable(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
        }
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String message = "If an account exists for that username, a password reset link has been sent.";
        String resetLink = null;
        User user = userRepository.findByUsername(request.username().trim()).orElse(null);
        if (user != null && user.getStatus() != UserStatus.DISABLED) {
            passwordResetTokenRepository.deleteByUserId(user.getId());
            String token = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(token);
            prt.setExpiresAt(Instant.now().plusSeconds(tokenValidHours * 3600L));
            passwordResetTokenRepository.save(prt);
            resetLink = passwordResetBaseUrl + "/#/reset-password?token=" + token;

            String email = null;
            if (user.getRole() == UserRole.BORROWER) {
                email = borrowerRepository.findByUserId(user.getId())
                    .map(Borrower::getEmail).orElse(null);
            } else {
                email = user.getEmail();
            }
            if (email != null && !email.isBlank()) {
                emailNotificationService.send(
                    email,
                    "Password reset – Loan Shark",
                    "Use this link to reset your password (valid for " + tokenValidHours + " hours):\n\n" + resetLink + "\n\nIf you did not request this, ignore this email."
                );
                resetLink = null;
                message = "If an account exists for that username and has an email, a reset link has been sent.";
            }
        }
        return new ForgotPasswordResponse(message, resetLink);
    }

    @Transactional
    public void resetPasswordWithToken(ResetPasswordWithTokenRequest request) {
        PasswordResetToken prt = passwordResetTokenRepository
                .findByTokenAndExpiresAtAfter(request.token().trim(), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,
                        "Invalid or expired reset link. Request a new one."));
        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(prt);
    }
}
