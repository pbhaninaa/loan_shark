package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerDocumentRequest;
import com.loanshark.api.dto.ApiDtos.BorrowerDocumentResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerRequest;
import com.loanshark.api.dto.ApiDtos.BorrowerResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerStatusUpdateRequest;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.BorrowerDocument;
import com.loanshark.api.entity.BorrowerStatus;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.BlacklistEntryRepository;
import com.loanshark.api.repository.BorrowerDocumentRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.BorrowerVerificationRepository;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.RiskAssessmentRepository;
import com.loanshark.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final BorrowerVerificationRepository borrowerVerificationRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final AuthService authService;
    private final SaIdValidationService saIdValidationService;
    private final NotificationService notificationService;

    public BorrowerService(
        BorrowerRepository borrowerRepository,
        BorrowerDocumentRepository borrowerDocumentRepository,
        BorrowerVerificationRepository borrowerVerificationRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        RiskAssessmentRepository riskAssessmentRepository,
        LoanRepository loanRepository,
        UserRepository userRepository,
        AuditLogService auditLogService,
        CurrentUserService currentUserService,
        AuthService authService,
        SaIdValidationService saIdValidationService,
        NotificationService notificationService
    ) {
        this.borrowerRepository = borrowerRepository;
        this.borrowerDocumentRepository = borrowerDocumentRepository;
        this.borrowerVerificationRepository = borrowerVerificationRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
        this.authService = authService;
        this.saIdValidationService = saIdValidationService;
        this.notificationService = notificationService;
    }

    public PageResponse<BorrowerResponse> listBorrowers(String query, int page, int size) {
        Page<Borrower> borrowerPage = borrowerRepository.search(
            query == null ? "" : query.trim(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PageResponse<>(
            borrowerPage.getContent().stream().map(this::toResponse).toList(),
            borrowerPage.getNumber(),
            borrowerPage.getSize(),
            borrowerPage.getTotalElements(),
            borrowerPage.getTotalPages()
        );
    }

    public BorrowerResponse getBorrower(UUID id) {
        Borrower borrower = findBorrower(id);
        enforceBorrowerOwnershipIfNeeded(borrower);
        return toResponse(borrower);
    }

    public BorrowerResponse getCurrentBorrower() {
        User currentUser = currentUserService.requireCurrentUser();
        Borrower borrower = borrowerRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Borrower profile not found"));
        return toResponse(borrower);
    }


    @Transactional
    public BorrowerResponse createBorrower(BorrowerRequest request) {
        ensureUnique(request.idNumber(), request.phone(), null);
        if (!saIdValidationService.isValid(request.idNumber())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid South African ID number");
        }
        Borrower borrower = new Borrower();
        applyRequest(borrower, request);
        if (request.username() != null && !request.username().isBlank()) {
            if (request.password() == null || request.password().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "Password is required when creating a borrower account");
            }
            borrower.setUser(authService.createUser(request.username(), request.password(), UserRole.BORROWER));
        }
        borrower.setStatus(BorrowerStatus.ACTIVE);
        Borrower saved = borrowerRepository.save(borrower);
        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(
            currentUser.getId(),
            "CREATE_BORROWER",
            "Borrower",
            saved.getId().toString(),
            saved.getUser() == null ? saved.getIdNumber() : "Borrower account created without image verification"
        );
        notificationService.notifyBorrowerProfileCreated(saved, saved.getUser() != null);
        return toResponse(saved);
    }

    @Transactional
    public BorrowerResponse updateBorrower(UUID id, BorrowerRequest request) {
        Borrower borrower = findBorrower(id);
        ensureUnique(request.idNumber(), request.phone(), borrower.getId());
        applyRequest(borrower, request);
        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPDATE_BORROWER", "Borrower", borrower.getId().toString(), borrower.getIdNumber());
        Borrower saved = borrowerRepository.save(borrower);
        if (saved.getUser() != null) {
            notificationService.notifyBorrowerProfileUpdated(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public BorrowerResponse updateBorrowerStatus(UUID id, BorrowerStatusUpdateRequest request) {
        Borrower borrower = findBorrower(id);
        borrower.setStatus(request.status());
        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPDATE_BORROWER_STATUS", "Borrower", borrower.getId().toString(), request.status().name());
        Borrower saved = borrowerRepository.save(borrower);
        notificationService.notifyBorrowerStatusChanged(saved);
        return toResponse(saved);
    }

    @Transactional
    public BorrowerDocumentResponse addDocument(UUID borrowerId, BorrowerDocumentRequest request) {
        Borrower borrower = findBorrower(borrowerId);
        enforceBorrowerOwnershipIfNeeded(borrower);
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(request.documentType());
        document.setFileUrl(request.fileUrl());
        BorrowerDocument saved = borrowerDocumentRepository.save(document);
        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPLOAD_DOCUMENT", "BorrowerDocument", saved.getId().toString(), request.documentType().name());
        return new BorrowerDocumentResponse(
            saved.getId(),
            saved.getDocumentType(),
            saved.getFileUrl(),
            saved.getOriginalFileName(),
            saved.getContentType(),
            saved.getFileSizeBytes(),
            saved.getUploadedAt()
        );
    }

    public List<BorrowerDocumentResponse> listDocuments(UUID borrowerId) {
        Borrower borrower = findBorrower(borrowerId);
        enforceBorrowerOwnershipIfNeeded(borrower);
        return borrowerDocumentRepository.findByBorrowerId(borrowerId).stream()
                .peek(doc -> System.out.println("DOC DEBUG: " + doc))
                .map(doc -> new BorrowerDocumentResponse(
                        doc.getId(),
                        doc.getDocumentType(),
                        doc.getFileUrl(),
                        doc.getOriginalFileName(),
                        doc.getContentType(),
                        doc.getFileSizeBytes(),
                        doc.getUploadedAt()
                ))
                .toList();
    }

    @Transactional
    public Borrower blacklistBorrower(UUID borrowerId) {
        Borrower borrower = findBorrower(borrowerId);
        borrower.setStatus(BorrowerStatus.BLACKLISTED);
        return borrowerRepository.save(borrower);
    }

    public boolean isBlacklisted(UUID borrowerId) {
        return blacklistEntryRepository.existsByBorrowerId(borrowerId);
    }

    /** Delete a client (borrower). Owner only. Fails if the client has any loans. Also deletes linked login user. */
    @Transactional
    public void deleteBorrower(UUID id) {
        Borrower borrower = findBorrower(id);
        if (!loanRepository.findByBorrowerIdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot delete client with existing loan history. Reject or clear loans first.");
        }
        UUID userId = borrower.getUser() != null ? borrower.getUser().getId() : null;

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "DELETE_BORROWER", "Borrower", id.toString(), borrower.getFirstName() + " " + borrower.getLastName());

        blacklistEntryRepository.deleteByBorrowerId(id);
        riskAssessmentRepository.deleteByBorrowerId(id);
        borrowerVerificationRepository.deleteByBorrowerId(id);
        borrowerDocumentRepository.deleteByBorrowerId(id);
        borrowerRepository.delete(borrower);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(userRepository::delete);
        }
    }

    public Borrower findBorrower(UUID id) {
        return borrowerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Borrower not found"));
    }

    private void enforceBorrowerOwnershipIfNeeded(Borrower borrower) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.BORROWER) {
            return;
        }

        UUID currentBorrowerId = borrowerRepository.findByUserId(currentUser.getId())
            .map(Borrower::getId)
            .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));

        if (!borrower.getId().equals(currentBorrowerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Borrowers can only access their own profile");
        }
    }

    private void ensureUnique(String idNumber, String phone, UUID borrowerId) {
        borrowerRepository.findByIdNumber(idNumber)
            .filter(existing -> !existing.getId().equals(borrowerId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(BAD_REQUEST, "ID number already exists");
            });
        borrowerRepository.findByPhone(phone)
            .filter(existing -> !existing.getId().equals(borrowerId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(BAD_REQUEST, "Phone already exists");
            });
    }

    private void applyRequest(Borrower borrower, BorrowerRequest request) {
        borrower.setFirstName(request.firstName());
        borrower.setLastName(request.lastName());
        borrower.setIdNumber(request.idNumber());
        borrower.setPhone(request.phone());
        borrower.setEmail(request.email());
        borrower.setAddress(request.address());
        borrower.setEmploymentStatus(request.employmentStatus());
        borrower.setMonthlyIncome(request.monthlyIncome());
        borrower.setEmployerName(request.employerName());
    }

    private BorrowerResponse toResponse(Borrower borrower) {
        return new BorrowerResponse(
            borrower.getId(),
            borrower.getFirstName(),
            borrower.getLastName(),
            borrower.getIdNumber(),
            borrower.getPhone(),
            borrower.getEmail(),
            borrower.getAddress(),
            borrower.getEmploymentStatus(),
            borrower.getMonthlyIncome(),
            borrower.getEmployerName(),
            borrower.getStatus(),
            borrower.getRiskScore()
        );
    }
}
