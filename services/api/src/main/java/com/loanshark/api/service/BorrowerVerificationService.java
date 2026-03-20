package com.loanshark.api.service;

import com.loanshark.api.dto.AdminCreateBorrowerWithDocsForm;
import com.loanshark.api.dto.ApiDtos.AuthResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerResponse;
import com.loanshark.api.dto.ApiDtos.VerificationResponse;
import com.loanshark.api.dto.BorrowerKycRegistrationForm;
import com.loanshark.api.entity.*;
import com.loanshark.api.repository.BorrowerDocumentRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.BorrowerVerificationRepository;
import com.loanshark.api.security.JwtService;
import com.loanshark.api.util.ValidationUtil;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class BorrowerVerificationService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerDocumentRepository borrowerDocumentRepository;
    private final BorrowerVerificationRepository borrowerVerificationRepository;
    private final AuthService authService;
    private final SaIdValidationService saIdValidationService;
    private final DocumentStorageService documentStorageService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final Environment environment;

    public BorrowerVerificationService(
            BorrowerRepository borrowerRepository,
            BorrowerDocumentRepository borrowerDocumentRepository,
            BorrowerVerificationRepository borrowerVerificationRepository,
            AuthService authService,
            SaIdValidationService saIdValidationService,
            DocumentStorageService documentStorageService,
            JwtService jwtService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            NotificationService notificationService, Environment environment) {
        this.borrowerRepository = borrowerRepository;
        this.borrowerDocumentRepository = borrowerDocumentRepository;
        this.borrowerVerificationRepository = borrowerVerificationRepository;
        this.authService = authService;
        this.saIdValidationService = saIdValidationService;
        this.documentStorageService = documentStorageService;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.environment = environment;
    }

    // ==========================
    // REGISTER BORROWER (KYC)
    // ==========================
    @Transactional
    public AuthResponse registerBorrower(BorrowerKycRegistrationForm form) {

        authService.ensureUsernameAvailable(form.getUsername());

        if (ValidationUtil.isAboveMinimumAge(form.getIdNumber(), 18)) {
            throw new ResponseStatusException(BAD_REQUEST, "Applicant must be at least 18 years old");
        }
        if (form.getMonthlyIncome().compareTo(new BigDecimal("1000")) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Income too low");
        }
        if (borrowerRepository.findByIdNumber(form.getIdNumber()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "ID already exists");
        }
        if (borrowerRepository.findByPhone(form.getPhone()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone already exists");
        }

        if(ValidationUtil.isValidSouthAfricanIdPdf(form.getIdDocument())){
            throw new ResponseStatusException(BAD_REQUEST, "Invalid SA ID document");
        }

        // ID PDF validation
//        MultipartFile idFile = form.getIdDocument();
//        if (idFile.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID document required");
//        }
//        try {
//            String base64Pdf = Base64.getEncoder().encodeToString(idFile.getBytes());
//            if (!ValidationUtil.isValidSouthAfricanIdPdf(base64Pdf)) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SA ID document");
//            }
//        } catch (IOException e) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error reading ID document");
//        }

        // Create user
        User user = authService.createUser(form.getUsername(), form.getPassword(), UserRole.BORROWER);

        // Create borrower
        Borrower borrower = new Borrower();
        borrower.setUser(user);
        borrower.setFirstName(form.getFirstName());
        borrower.setLastName(form.getLastName());
        borrower.setIdNumber(form.getIdNumber());
        borrower.setPhone(form.getPhone());
        borrower.setEmail(form.getEmail());
        borrower.setAddress(form.getAddress());
        borrower.setEmploymentStatus(form.getEmploymentStatus());
        borrower.setMonthlyIncome(form.getMonthlyIncome());
        borrower.setEmployerName(form.getEmployerName());
        borrower.setStatus(BorrowerStatus.PENDING_VERIFICATION);
        borrower = borrowerRepository.save(borrower);

        // Save documents
        BorrowerDocument idDocument;
        BorrowerDocument selfieDocument;
        try {
            idDocument = createDocument(borrower, DocumentType.ID_COPY,
                    documentStorageService.storePdf(form.getIdDocument()), form.getIdDocument().getBytes());
            selfieDocument = createDocument(borrower, DocumentType.SELFIE,
                    documentStorageService.storeImage(form.getSelfieImage()), form.getSelfieImage().getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "File processing failed");
        }

        // Verification
        VerificationComputation result = computeVerification(form, idDocument, selfieDocument);

        BorrowerVerification verification = new BorrowerVerification();
        verification.setBorrower(borrower);
        verification.setStatus(result.status());
        verification.setIdDocument(idDocument);
        verification.setSelfieDocument(selfieDocument);
        verification.setSaIdValid(result.saIdValid());
        verification.setOcrConfidence(result.ocrConfidence());
        verification.setDetailsMatched(result.detailsMatched());
        verification.setFaceMatchScore(result.faceMatchScore());
        verification.setFaceMatched(result.faceMatched());
        verification.setReviewNotes(result.notes());
        borrowerVerificationRepository.save(verification);

        borrower.setStatus(mapBorrowerStatus(result.status()));
        borrowerRepository.save(borrower);

        // Return auth response
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                borrower.getId()
        );
    }
    public Borrower requireActiveBorrowerAccess(UUID borrowerId) {
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUser.getRole() != UserRole.BORROWER) {
            throw new ResponseStatusException(FORBIDDEN, "Only borrowers can access this resource");
        }

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Borrower not found"));

        if (!borrower.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied to this borrower");
        }

        if (borrower.getStatus() != BorrowerStatus.ACTIVE) {
            throw new ResponseStatusException(FORBIDDEN, "Borrower is not active");
        }

        return borrower;
    }
    // ==========================
    // ADMIN CREATES BORROWER WITH DOCUMENTS
    // ==========================
    @Transactional
    public BorrowerResponse createBorrowerWithDocuments(AdminCreateBorrowerWithDocsForm form) {
        authService.ensureUsernameAvailable(form.getUsername());
        if (borrowerRepository.findByIdNumber(form.getIdNumber()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "ID number already exists");
        }
        if (borrowerRepository.findByPhone(form.getPhone()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone already exists");
        }
        if (!saIdValidationService.isValid(form.getIdNumber())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid SA ID number");
        }

        User user = authService.createUser(form.getUsername(), form.getPassword(), UserRole.BORROWER);

        Borrower borrower = new Borrower();
        borrower.setUser(user);
        borrower.setFirstName(form.getFirstName());
        borrower.setLastName(form.getLastName());
        borrower.setIdNumber(form.getIdNumber());
        borrower.setPhone(form.getPhone());
        borrower.setEmail(form.getEmail());
        borrower.setAddress(form.getAddress());
        borrower.setEmploymentStatus(form.getEmploymentStatus());
        borrower.setMonthlyIncome(form.getMonthlyIncome() != null ? form.getMonthlyIncome() : BigDecimal.ZERO);
        borrower.setEmployerName(form.getEmployerName());
        borrower.setStatus(BorrowerStatus.PENDING_VERIFICATION);
        borrower.setRiskScore(0);
        borrower = borrowerRepository.save(borrower);

        BorrowerDocument idDocument;
        BorrowerDocument selfieDocument;
        try {
            idDocument = createDocument(borrower, DocumentType.ID_COPY,
                    documentStorageService.storePdf(form.getIdDocument()), form.getIdDocument().getBytes());
            selfieDocument = createDocument(borrower, DocumentType.SELFIE,
                    documentStorageService.storeImage(form.getSelfieImage()), form.getSelfieImage().getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "File processing failed");
        }

        BorrowerVerification verification = new BorrowerVerification();
        verification.setBorrower(borrower);
        verification.setStatus(VerificationStatus.MANUAL_REVIEW);
        verification.setIdDocument(idDocument);
        verification.setSelfieDocument(selfieDocument);
        verification.setSaIdValid(true);
        verification.setDetailsMatched(false);
        verification.setFaceMatched(false);
        verification.setReviewNotes("Admin-created with uploaded documents. Manual verification required.");
        borrowerVerificationRepository.save(verification);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "CREATE_BORROWER_WITH_DOCS", "Borrower",
                borrower.getId().toString(), "Pending owner verification");
        notificationService.notifyBorrowerProfileCreated(borrower, true);
        notificationService.notifyUser(user.getId(), "KYC",
                "Profile created by admin. Verification pending review.");

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

    // ==========================
    // VERIFICATION ACCESS METHODS
    // ==========================
    @Transactional(readOnly = true)
    public VerificationResponse myVerification() {
        User currentUser = currentUserService.requireCurrentUser();
        BorrowerVerification verification = borrowerVerificationRepository
                .findTopByBorrowerUserIdOrderByCreatedAtDesc(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification not found"));
        return toResponse(verification);
    }

    @Transactional(readOnly = true)
    public List<VerificationResponse> pendingReviews() {
        return borrowerVerificationRepository.findByStatusOrderByCreatedAtDesc(VerificationStatus.MANUAL_REVIEW)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VerificationResponse getByBorrowerId(UUID borrowerId) {
        BorrowerVerification verification = borrowerVerificationRepository
                .findTopByBorrowerIdOrderByCreatedAtDesc(borrowerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No verification found for this borrower"));
        return toResponse(verification);
    }

    @Transactional
    public VerificationResponse approve(UUID verificationId, String notes) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        verification.setStatus(VerificationStatus.APPROVED);
        verification.setReviewNotes(notes);
        verification.setReviewedBy(currentUserService.requireCurrentUser());
        verification.setReviewedAt(Instant.now());

        Borrower borrower = verification.getBorrower();
        borrower.setStatus(BorrowerStatus.ACTIVE);
        borrowerRepository.save(borrower);

        borrowerVerificationRepository.save(verification);
        notificationService.notifyUser(borrower.getUser().getId(), "KYC", "Verification approved.");
        auditLogService.record(verification.getReviewedBy().getId(), "APPROVE_VERIFICATION",
                "BorrowerVerification", verification.getId().toString(), notes);

        return toResponse(verification);
    }

    @Transactional
    public VerificationResponse reject(UUID verificationId, String notes) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        verification.setStatus(VerificationStatus.REJECTED);
        verification.setReviewNotes(notes);
        verification.setReviewedBy(currentUserService.requireCurrentUser());
        verification.setReviewedAt(Instant.now());

        Borrower borrower = verification.getBorrower();
        borrower.setStatus(BorrowerStatus.VERIFICATION_REJECTED);
        borrowerRepository.save(borrower);

        borrowerVerificationRepository.save(verification);
        notificationService.notifyUser(borrower.getUser().getId(), "KYC",
                "Verification rejected. " + notes);
        auditLogService.record(verification.getReviewedBy().getId(), "REJECT_VERIFICATION",
                "BorrowerVerification", verification.getId().toString(), notes);

        return toResponse(verification);
    }

    // ==========================
    // DOCUMENT METHODS
    // ==========================
    @Transactional(readOnly = true)
    public VerificationDocumentPayload idDocumentContent(UUID verificationId) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        return readDocument(verification.getIdDocument());
    }

    @Transactional(readOnly = true)
    public VerificationDocumentPayload selfieDocumentContent(UUID verificationId) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        return readDocument(verification.getSelfieDocument());
    }

    private BorrowerDocument createDocument(Borrower borrower, DocumentType documentType,
                                            DocumentStorageService.StoredFile storedFile, byte[] fileBytes) {
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(documentType);
        document.setFileUrl(storedFile.path());
        document.setOriginalFileName(storedFile.originalFileName());
        document.setContentType(storedFile.contentType());
        document.setFileSizeBytes(storedFile.size());
        document.setSha256Checksum(storedFile.sha256());
        document.setFileData(fileBytes);
        return borrowerDocumentRepository.save(document);
    }

    private VerificationComputation computeVerification(BorrowerKycRegistrationForm form,
                                                        BorrowerDocument idDocument,
                                                        BorrowerDocument selfieDocument) {

        boolean saIdValid = saIdValidationService.isValid(form.getIdNumber());

        String extractedText = ValidationUtil.extractPdfText(Path.of(idDocument.getFileUrl()));
        BigDecimal ocrConfidence = extractedText.isBlank() ? BigDecimal.ZERO : new BigDecimal("70.00");

        String normalizedText = ValidationUtil.normalize(extractedText);
        boolean firstNameMatch = normalizedText.contains(ValidationUtil.normalize(form.getFirstName()));
        boolean lastNameMatch = normalizedText.contains(ValidationUtil.normalize(form.getLastName()));
        boolean idMatch = normalizedText.contains(ValidationUtil.normalize(form.getIdNumber()));

        boolean detailsMatched = firstNameMatch && lastNameMatch && idMatch;

        BigDecimal faceScore = ValidationUtil.compareSelfieWithId(Path.of(idDocument.getFileUrl()),
                Path.of(selfieDocument.getFileUrl()));
        boolean faceMatched = faceScore.compareTo(new BigDecimal("78.00")) >= 0;

        VerificationStatus status;
        String notes;
        if (!saIdValid) {
            status = VerificationStatus.REJECTED;
            notes = "Invalid SA ID number";
        } else if (detailsMatched && faceMatched) {
            status = VerificationStatus.APPROVED;
            notes = "Auto-approved: all checks passed";
        } else {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Manual review required";
        }

        return new VerificationComputation(status, saIdValid, ocrConfidence,
                form.getFirstName(), form.getLastName(),
                idMatch ? form.getIdNumber() : null,
                detailsMatched, faceScore, faceMatched, notes);
    }

    // Updated toResponse method
    private VerificationResponse toResponse(BorrowerVerification verification) {
        List<BorrowerDocument> documents = borrowerDocumentRepository.findByBorrowerId(verification.getBorrower().getId());

        BorrowerDocument idDocument = documents.stream()
                .filter(doc -> doc.getDocumentType() == DocumentType.ID_COPY)
                .findFirst().orElse(null);

        BorrowerDocument selfieDocument = documents.stream()
                .filter(doc -> doc.getDocumentType() == DocumentType.SELFIE)
                .findFirst().orElse(null);

        String baseUrl = environment.getProperty("app.base-url", "http://localhost:8080");

        return new VerificationResponse(
                verification.getId(),
                verification.getBorrower().getId(),
                verification.getStatus(),
                idDocument != null ? idDocument.getId() : null,
                idDocument != null ? idDocument.getOriginalFileName() : null,
                idDocument != null ? idDocument.getContentType() : null,
                idDocument != null ? baseUrl + "/borrowers/documents/" + idDocument.getId() + "/file" : null,
                selfieDocument != null ? selfieDocument.getId() : null,
                selfieDocument != null ? selfieDocument.getOriginalFileName() : null,
                selfieDocument != null ? selfieDocument.getContentType() : null,
                selfieDocument != null ? baseUrl + "/borrowers/documents/" + selfieDocument.getId() + "/file" : null,
                verification.isSaIdValid(),
                verification.getLatitude(),
                verification.getLongitude(),
                verification.getLocationCapturedAt(),
                verification.getLocationName(),
                verification.getExtractedFirstName(),
                verification.getExtractedLastName(),
                verification.getExtractedIdNumber(),
                verification.getOcrConfidence(),
                verification.isDetailsMatched(),
                verification.getFaceMatchScore(),
                verification.isFaceMatched(),
                verification.getReviewNotes(),
                verification.getReviewedBy() != null ? verification.getReviewedBy().getUsername() : null,
                verification.getReviewedAt(),
                verification.getCreatedAt(),
                verification.getUpdatedAt()
        );
    }
    private VerificationDocumentPayload readDocument(BorrowerDocument document) {
        try {
            byte[] content = document.getFileData() != null ? document.getFileData() :
                    Files.readAllBytes(Path.of(document.getFileUrl()));
            return new VerificationDocumentPayload(
                    document.getOriginalFileName(),
                    document.getContentType(),
                    content
            );
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied");
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "Document not found");
        }
    }

    private BorrowerVerification requireOwnerReview(UUID verificationId) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can review verifications");
        }
        return borrowerVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification record not found"));
    }

    private BorrowerStatus mapBorrowerStatus(VerificationStatus status) {
        return switch (status) {
            case APPROVED -> BorrowerStatus.ACTIVE;
            case MANUAL_REVIEW -> BorrowerStatus.MANUAL_REVIEW;
            case REJECTED -> BorrowerStatus.VERIFICATION_REJECTED;
            case PENDING -> BorrowerStatus.PENDING_VERIFICATION;
        };
    }

    // ==========================
    // RECORDS
    // ==========================
    private record VerificationComputation(
            VerificationStatus status,
            boolean saIdValid,
            BigDecimal ocrConfidence,
            String extractedFirstName,
            String extractedLastName,
            String extractedIdNumber,
            boolean detailsMatched,
            BigDecimal faceMatchScore,
            boolean faceMatched,
            String notes
    ) {}

    public record VerificationDocumentPayload(
            String fileName,
            String contentType,
            byte[] content
    ) {}

}