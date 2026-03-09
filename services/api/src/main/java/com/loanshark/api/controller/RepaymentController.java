package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.RepaymentRequest;
import com.loanshark.api.dto.ApiDtos.RepaymentResponse;
import com.loanshark.api.service.RepaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @GetMapping("/next-reference")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public Map<String, String> nextReference() {
        return Map.of("nextReference", repaymentService.getNextReferenceNumber());
    }

    /** List all payment history (staff: all loans; borrower: own loans). Optional ?loanId= filters to one loan. */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public PageResponse<RepaymentResponse> list(
        @RequestParam(required = false) java.util.UUID loanId,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        if (loanId != null) {
            return repaymentService.listByLoan(loanId, q, page, size);
        }
        return repaymentService.listAll(q, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RepaymentResponse create(@Valid @RequestBody RepaymentRequest request) {
        return repaymentService.record(request);
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public PageResponse<RepaymentResponse> list(
        @PathVariable UUID loanId,
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return repaymentService.listByLoan(loanId, q, page, size);
    }
}
