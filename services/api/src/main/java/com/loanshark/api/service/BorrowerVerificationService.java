package com.loanshark.api.service;

import com.loanshark.api.dto.AdminCreateBorrowerWithDocsForm;
import com.loanshark.api.dto.ApiDtos.AuthResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerResponse;
import com.loanshark.api.dto.ApiDtos.VerificationResponse;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.BorrowerDocument;
import com.loanshark.api.entity.BorrowerStatus;
import com.loanshark.api.entity.BorrowerVerification;
import com.loanshark.api.entity.DocumentType;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.entity.VerificationStatus;
import com.loanshark.api.repository.BorrowerDocumentRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.BorrowerVerificationRepository;
import com.loanshark.api.security.JwtService;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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
            NotificationService notificationService) {
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
    }

    @Transactional
    public AuthResponse registerBorrower(com.loanshark.api.dto.BorrowerKycRegistrationForm form) {
        authService.ensureUsernameAvailable(form.getUsername());
        if (borrowerRepository.findByIdNumber(form.getIdNumber()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "ID number already exists");
        }
        if (borrowerRepository.findByPhone(form.getPhone()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone already exists");
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
        borrower.setMonthlyIncome(form.getMonthlyIncome());
        borrower.setEmployerName(form.getEmployerName());
        borrower.setStatus(BorrowerStatus.PENDING_VERIFICATION);
        borrower = borrowerRepository.save(borrower);

        BorrowerDocument idDocument = createDocument(
                borrower,
                DocumentType.ID_COPY,
                documentStorageService.storePdf(form.getIdDocument()));
        BorrowerDocument selfieDocument = createDocument(
                borrower,
                DocumentType.SELFIE,
                documentStorageService.storeImage(form.getSelfieImage()));

        VerificationComputation result = computeVerification(form, idDocument, selfieDocument);

        BorrowerVerification verification = new BorrowerVerification();
        verification.setBorrower(borrower);
        verification.setStatus(result.status());
        verification.setIdDocument(idDocument);
        verification.setSelfieDocument(selfieDocument);
        verification.setLatitude(form.getLatitude());
        verification.setLongitude(form.getLongitude());
        verification.setLocationCapturedAt(Instant.now());
        verification.setLocationName(form.getLocationName());
        verification.setSaIdValid(result.saIdValid());
        verification.setOcrConfidence(result.ocrConfidence());
        verification.setExtractedFirstName(result.extractedFirstName());
        verification.setExtractedLastName(result.extractedLastName());
        verification.setExtractedIdNumber(result.extractedIdNumber());
        verification.setDetailsMatched(result.detailsMatched());
        verification.setFaceMatchScore(result.faceMatchScore());
        verification.setFaceMatched(result.faceMatched());
        verification.setReviewNotes(result.notes());
        borrowerVerificationRepository.save(verification);

        borrower.setStatus(mapBorrowerStatus(result.status()));
        borrowerRepository.save(borrower);

        auditLogService.record(user.getId(), "REGISTER_BORROWER_KYC", "Borrower", borrower.getId(), result.notes());
        notificationService.notifyBorrowerProfileCreated(borrower, false);
        notificationService.notifyUser(user.getId(), "KYC", result.notes());

        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getUsername(), user.getRole(), borrower.getId());
    }

    /**
     * Admin/staff creates a client with ID PDF and photo uploaded from PC. No live
     * selfie/location.
     * Creates borrower with PENDING_VERIFICATION and a verification in
     * MANUAL_REVIEW for owner to approve.
     */
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
            throw new ResponseStatusException(BAD_REQUEST, "Invalid South African ID number");
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
        borrower.setMonthlyIncome(
                form.getMonthlyIncome() != null ? form.getMonthlyIncome() : java.math.BigDecimal.ZERO);
        borrower.setEmployerName(form.getEmployerName());
        borrower.setStatus(BorrowerStatus.PENDING_VERIFICATION);
        borrower.setRiskScore(0);
        borrower = borrowerRepository.save(borrower);

        BorrowerDocument idDocument = createDocument(
                borrower,
                DocumentType.ID_COPY,
                documentStorageService.storePdf(form.getIdDocument()));
        BorrowerDocument selfieDocument = createDocument(
                borrower,
                DocumentType.SELFIE,
                documentStorageService.storeImage(form.getSelfieImage()));

        BorrowerVerification verification = new BorrowerVerification();
        verification.setBorrower(borrower);
        verification.setStatus(VerificationStatus.MANUAL_REVIEW);
        verification.setIdDocument(idDocument);
        verification.setSelfieDocument(selfieDocument);
        verification.setSaIdValid(true);
        verification.setDetailsMatched(false);
        verification.setFaceMatched(false);
        verification.setReviewNotes("Admin-created with uploaded documents. Please verify ID and photo manually.");
        borrowerVerificationRepository.save(verification);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "CREATE_BORROWER_WITH_DOCS", "Borrower", borrower.getId(),
                "Pending owner verification");
        notificationService.notifyBorrowerProfileCreated(borrower, true);
        notificationService.notifyUser(user.getId(), "KYC",
                "Profile created by admin. Your verification is pending owner review.");

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
                borrower.getRiskScore());
    }

    @Transactional(readOnly = true)
    public VerificationResponse myVerification() {
        User currentUser = currentUserService.requireCurrentUser();
        BorrowerVerification verification = borrowerVerificationRepository
                .findTopByBorrowerUserIdOrderByCreatedAtDesc(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification record not found"));
        return toResponse(verification);
    }

    @Transactional(readOnly = true)
    public java.util.List<VerificationResponse> pendingReviews() {
        return borrowerVerificationRepository.findByStatusOrderByCreatedAtDesc(VerificationStatus.MANUAL_REVIEW)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VerificationResponse approve(Long verificationId, String notes) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        verification.setStatus(VerificationStatus.APPROVED);
        verification.setReviewNotes(notes);
        verification.setReviewedBy(currentUserService.requireCurrentUser());
        verification.setReviewedAt(Instant.now());
        Borrower borrower = verification.getBorrower();
        borrower.setStatus(BorrowerStatus.ACTIVE);
        borrowerRepository.save(borrower);
        notificationService.notifyUser(borrower.getUser().getId(), "KYC", "Your verification has been approved.");
        auditLogService.record(verification.getReviewedBy().getId(), "APPROVE_VERIFICATION", "BorrowerVerification",
                verification.getId(), notes);
        return toResponse(borrowerVerificationRepository.save(verification));
    }

    @Transactional
    public VerificationResponse reject(Long verificationId, String notes) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        verification.setStatus(VerificationStatus.REJECTED);
        verification.setReviewNotes(notes);
        verification.setReviewedBy(currentUserService.requireCurrentUser());
        verification.setReviewedAt(Instant.now());
        Borrower borrower = verification.getBorrower();
        borrower.setStatus(BorrowerStatus.VERIFICATION_REJECTED);
        borrowerRepository.save(borrower);
        notificationService.notifyUser(borrower.getUser().getId(), "KYC",
                "Your verification has been rejected. " + notes);
        auditLogService.record(verification.getReviewedBy().getId(), "REJECT_VERIFICATION", "BorrowerVerification",
                verification.getId(), notes);
        return toResponse(borrowerVerificationRepository.save(verification));
    }

    @Transactional(readOnly = true)
    public Optional<VerificationResponse> getByBorrowerId(Long borrowerId) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can view borrower verification");
        }
        return borrowerVerificationRepository.findTopByBorrowerIdOrderByCreatedAtDesc(borrowerId)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VerificationDocumentPayload idDocumentContent(Long verificationId) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        return readDocument(verification.getIdDocument());
    }

    @Transactional(readOnly = true)
    public VerificationDocumentPayload selfieDocumentContent(Long verificationId) {
        BorrowerVerification verification = requireOwnerReview(verificationId);
        return readDocument(verification.getSelfieDocument());
    }

    public void requireActiveBorrowerAccess(User currentUser) {
        if (currentUser.getRole() != UserRole.BORROWER) {
            return;
        }
        Borrower borrower = borrowerRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
        if (borrower.getStatus() != BorrowerStatus.ACTIVE) {
            throw new ResponseStatusException(FORBIDDEN, "Borrower verification is not complete");
        }
    }

    private BorrowerVerification requireOwnerReview(Long verificationId) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can review verifications");
        }
        return borrowerVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification record not found"));
    }

    private BorrowerDocument createDocument(Borrower borrower, DocumentType documentType,
            DocumentStorageService.StoredFile storedFile) {
        BorrowerDocument document = new BorrowerDocument();
        document.setBorrower(borrower);
        document.setDocumentType(documentType);
        document.setFileUrl(storedFile.path());
        document.setOriginalFileName(storedFile.originalFileName());
        document.setContentType(storedFile.contentType());
        document.setFileSizeBytes(storedFile.size());
        document.setSha256Checksum(storedFile.sha256());
        return borrowerDocumentRepository.save(document);
    }

    private VerificationComputation computeVerification(
            com.loanshark.api.dto.BorrowerKycRegistrationForm form,
            BorrowerDocument idDocument,
            BorrowerDocument selfieDocument) {
        boolean saIdValid = saIdValidationService.isValid(form.getIdNumber());
        String extractedText = extractPdfText(Path.of(idDocument.getFileUrl()));
        BigDecimal ocrConfidence = extractedText.isBlank() ? new BigDecimal("0.00") : new BigDecimal("70.00");

        String normalizedText = normalize(extractedText);
        boolean firstNameMatch = normalizedText.contains(normalize(form.getFirstName()));
        boolean lastNameMatch = normalizedText.contains(normalize(form.getLastName()));
        boolean idNumberMatch = normalizedText.contains(normalize(form.getIdNumber()));
        boolean detailsMatched = firstNameMatch && lastNameMatch && idNumberMatch;

        BigDecimal faceScore = compareSelfieWithId(Path.of(idDocument.getFileUrl()),
                Path.of(selfieDocument.getFileUrl()));
        boolean faceMatched = faceScore.compareTo(new BigDecimal("78.00")) >= 0;

        VerificationStatus status;
        String notes;
        if (!saIdValid) {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created, but owner review is required because the SA ID number failed validation.";
        } else if (detailsMatched && faceMatched) {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created and automatic checks passed. Owner review is still required before system access is granted.";
        } else if (!detailsMatched && !faceMatched) {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created, but owner review is required because the provided details do not match the ID copy and the selfie does not match the ID photo.";
        } else if (!detailsMatched) {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created, but owner review is required because the provided details do not match the information extracted from the ID copy.";
        } else if (!faceMatched) {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created, but owner review is required because the selfie does not match the photo on the ID copy.";
        } else {
            status = VerificationStatus.MANUAL_REVIEW;
            notes = "Profile created, but owner review is required because the identity checks did not fully pass.";
        }

        return new VerificationComputation(
                status,
                saIdValid,
                ocrConfidence,
                extractField(extractedText, form.getFirstName()),
                extractField(extractedText, form.getLastName()),
                idNumberMatch ? form.getIdNumber() : null,
                detailsMatched,
                faceScore,
                faceMatched,
                notes);
    }

    private String extractPdfText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException exception) {
            return "";
        }
    }

    private BigDecimal compareSelfieWithId(Path pdfPath, Path selfiePath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage idImage = renderer.renderImageWithDPI(0, 140);
            BufferedImage selfieImage = javax.imageio.ImageIO.read(selfiePath.toFile());
            if (selfieImage == null) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            long firstHash = averageHash(cropCenter(idImage));
            long secondHash = averageHash(cropCenter(selfieImage));
            int distance = Long.bitCount(firstHash ^ secondHash);
            double similarity = ((64.0 - distance) / 64.0) * 100.0;
            return BigDecimal.valueOf(Math.max(0.0, similarity)).setScale(2, RoundingMode.HALF_UP);
        } catch (IOException exception) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BufferedImage cropCenter(BufferedImage source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = Math.max(0, (source.getWidth() - size) / 2);
        int y = Math.max(0, (source.getHeight() - size) / 2);
        BufferedImage cropped = source.getSubimage(x, y, size, size);
        Image scaled = cropped.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        BufferedImage gray = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(gray, null);
    }

    private long averageHash(BufferedImage image) {
        long total = 0;
        int[] pixels = new int[32 * 32];
        image.getRaster().getPixels(0, 0, 32, 32, pixels);
        for (int pixel : pixels) {
            total += pixel;
        }
        long average = total / pixels.length;
        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (pixels[i] >= average) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    private String extractField(String text, String providedValue) {
        return normalize(text).contains(normalize(providedValue)) ? providedValue : null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private BorrowerStatus mapBorrowerStatus(VerificationStatus status) {
        return switch (status) {
            case APPROVED -> BorrowerStatus.ACTIVE;
            case MANUAL_REVIEW -> BorrowerStatus.MANUAL_REVIEW;
            case REJECTED -> BorrowerStatus.VERIFICATION_REJECTED;
            case PENDING -> BorrowerStatus.PENDING_VERIFICATION;
        };
    }

    private VerificationResponse toResponse(BorrowerVerification verification) {
        return new VerificationResponse(
                verification.getId(),
                verification.getBorrower().getId(),
                verification.getStatus(),
                verification.getIdDocument().getId(),
                verification.getIdDocument().getOriginalFileName(),
                verification.getIdDocument().getContentType(),
                verification.getSelfieDocument().getId(),
                verification.getSelfieDocument().getOriginalFileName(),
                verification.getSelfieDocument().getContentType(),
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
                verification.getReviewedBy() == null ? null : verification.getReviewedBy().getUsername(),
                verification.getReviewedAt(),
                verification.getCreatedAt(),
                verification.getUpdatedAt());
    }

    private VerificationDocumentPayload readDocument(BorrowerDocument document) {
        try {
            return new VerificationDocumentPayload(
                    document.getOriginalFileName(),
                    document.getContentType(),
                    Files.readAllBytes(Path.of(document.getFileUrl())));
        } catch (AccessDeniedException exception) {
            throw new ResponseStatusException(FORBIDDEN, "You are not allowed to read this document");
        } catch (IOException exception) {
            throw new ResponseStatusException(NOT_FOUND, "Verification document could not be read");
        }
    }

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
            String notes) {
    }

    public record VerificationDocumentPayload(
            String fileName,
            String contentType,
            byte[] content) {
    }
}
