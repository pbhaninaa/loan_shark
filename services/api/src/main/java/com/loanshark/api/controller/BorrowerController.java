package com.loanshark.api.controller;

import com.loanshark.api.dto.AdminCreateBorrowerWithDocsForm;
import com.loanshark.api.dto.ApiDtos.*;
import com.loanshark.api.entity.BorrowerDocument;
import com.loanshark.api.repository.BorrowerDocumentRepository;
import com.loanshark.api.service.BorrowerService;
import com.loanshark.api.service.BorrowerVerificationService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/borrowers")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final BorrowerVerificationService borrowerVerificationService;
    private final BorrowerDocumentRepository borrowerDocumentRepository;

    public BorrowerController(BorrowerService borrowerService,
                              BorrowerVerificationService borrowerVerificationService,
                              BorrowerDocumentRepository borrowerDocumentRepository) {
        this.borrowerService = borrowerService;
        this.borrowerVerificationService = borrowerVerificationService;
        this.borrowerDocumentRepository = borrowerDocumentRepository;
    }

    // ------------------- Borrower Endpoints -------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public PageResponse<BorrowerResponse> list(@RequestParam(defaultValue = "") String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return borrowerService.listBorrowers(q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public BorrowerResponse get(@PathVariable UUID id) {
        return borrowerService.getBorrower(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BORROWER')")
    public BorrowerResponse me() {
        return borrowerService.getCurrentBorrower();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowerResponse create(@Valid @RequestBody BorrowerRequest request) {
        return borrowerService.createBorrower(request);
    }

    @PostMapping(value = "/with-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowerResponse createWithDocuments(@Valid @ModelAttribute AdminCreateBorrowerWithDocsForm form) {
        return borrowerVerificationService.createBorrowerWithDocuments(form);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public BorrowerResponse update(@PathVariable UUID id, @Valid @RequestBody BorrowerRequest request) {
        return borrowerService.updateBorrower(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public BorrowerResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody BorrowerStatusUpdateRequest request) {
        return borrowerService.updateBorrowerStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        borrowerService.deleteBorrower(id);
    }

    // ------------------- Borrower Document Endpoints -------------------

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public List<BorrowerDocumentResponse> listDocuments(@PathVariable UUID id) {
        return borrowerService.listDocuments(id);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowerDocumentResponse addDocument(@PathVariable UUID id, @Valid @RequestBody BorrowerDocumentRequest request) {
        return borrowerService.addDocument(id, request);
    }

    // ------------------- Serve Document Files -------------------

    @GetMapping("/documents/{documentId}/file")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public ResponseEntity<Resource> getDocumentFile(@PathVariable UUID documentId) throws Exception {
        BorrowerDocument document = borrowerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFileUrl()).toAbsolutePath().normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not found or not readable");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + document.getOriginalFileName() + "\"")
                .body(resource);
    }
}