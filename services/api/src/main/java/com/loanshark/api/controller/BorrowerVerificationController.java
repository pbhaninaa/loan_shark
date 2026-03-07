package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.VerificationResponse;
import com.loanshark.api.dto.ApiDtos.VerificationReviewRequest;
import com.loanshark.api.service.BorrowerVerificationService.VerificationDocumentPayload;
import com.loanshark.api.service.BorrowerVerificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/verifications")
public class BorrowerVerificationController {

    private final BorrowerVerificationService borrowerVerificationService;

    public BorrowerVerificationController(BorrowerVerificationService borrowerVerificationService) {
        this.borrowerVerificationService = borrowerVerificationService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BORROWER')")
    public VerificationResponse myVerification() {
        return borrowerVerificationService.myVerification();
    }

    @GetMapping("/manual-review")
    @PreAuthorize("hasRole('OWNER')")
    public List<VerificationResponse> pendingManualReview() {
        return borrowerVerificationService.pendingReviews();
    }

    @GetMapping("/by-borrower/{borrowerId}")
    @PreAuthorize("hasRole('OWNER')")
    public VerificationResponse getByBorrowerId(@PathVariable Long borrowerId) {
        return borrowerVerificationService.getByBorrowerId(borrowerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No verification found for this borrower"));
    }

    @PostMapping("/{verificationId}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public VerificationResponse approve(
        @PathVariable Long verificationId,
        @Valid @RequestBody VerificationReviewRequest request
    ) {
        return borrowerVerificationService.approve(verificationId, request.notes());
    }

    @PostMapping("/{verificationId}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public VerificationResponse reject(
        @PathVariable Long verificationId,
        @Valid @RequestBody VerificationReviewRequest request
    ) {
        return borrowerVerificationService.reject(verificationId, request.notes());
    }

    @GetMapping("/{verificationId}/id-document")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> idDocument(@PathVariable Long verificationId) {
        return documentResponse(borrowerVerificationService.idDocumentContent(verificationId));
    }

    @GetMapping("/{verificationId}/selfie-document")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> selfieDocument(@PathVariable Long verificationId) {
        return documentResponse(borrowerVerificationService.selfieDocumentContent(verificationId));
    }

    private ResponseEntity<byte[]> documentResponse(VerificationDocumentPayload payload) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (payload.contentType() != null && !payload.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(payload.contentType());
        }
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(payload.fileName()).build().toString()
            )
            .contentType(mediaType)
            .body(payload.content());
    }
}
