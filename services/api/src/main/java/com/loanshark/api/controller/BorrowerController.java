package com.loanshark.api.controller;

import com.loanshark.api.dto.AdminCreateBorrowerWithDocsForm;
import com.loanshark.api.dto.ApiDtos.BorrowerDocumentRequest;
import com.loanshark.api.dto.ApiDtos.BorrowerDocumentResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerRequest;
import com.loanshark.api.dto.ApiDtos.BorrowerResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerStatusUpdateRequest;
import com.loanshark.api.service.BorrowerService;
import com.loanshark.api.service.BorrowerVerificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final BorrowerVerificationService borrowerVerificationService;

    public BorrowerController(BorrowerService borrowerService, BorrowerVerificationService borrowerVerificationService) {
        this.borrowerService = borrowerService;
        this.borrowerVerificationService = borrowerVerificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public PageResponse<BorrowerResponse> list(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return borrowerService.listBorrowers(q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public BorrowerResponse get(@PathVariable Long id) {
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
    public BorrowerResponse update(@PathVariable Long id, @Valid @RequestBody BorrowerRequest request) {
        return borrowerService.updateBorrower(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public BorrowerResponse updateStatus(@PathVariable Long id, @Valid @RequestBody BorrowerStatusUpdateRequest request) {
        return borrowerService.updateBorrowerStatus(id, request);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public List<BorrowerDocumentResponse> listDocuments(@PathVariable Long id) {
        return borrowerService.listDocuments(id);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowerDocumentResponse addDocument(@PathVariable Long id, @Valid @RequestBody BorrowerDocumentRequest request) {
        return borrowerService.addDocument(id, request);
    }
}
