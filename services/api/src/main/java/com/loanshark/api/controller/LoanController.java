package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.LoanApplicationRequest;
import com.loanshark.api.dto.ApiDtos.LoanDecisionRequest;
import com.loanshark.api.dto.ApiDtos.LoanResponse;
import com.loanshark.api.dto.ApiDtos.LoanUpdateRequest;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.ScheduleResponse;
import com.loanshark.api.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse apply(@Valid @RequestBody LoanApplicationRequest request) {
        return loanService.apply(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public PageResponse<LoanResponse> list(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return loanService.listAll(q, page, size);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BORROWER')")
    public PageResponse<LoanResponse> myLoans(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return loanService.listMyLoans(q, page, size);
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public LoanResponse get(@PathVariable UUID loanId) {
        return loanService.getLoan(loanId);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public LoanResponse approve(@Valid @RequestBody LoanDecisionRequest request) {
        return loanService.approve(request);
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public LoanResponse reject(@Valid @RequestBody LoanDecisionRequest request) {
        return loanService.reject(request);
    }

    @GetMapping("/{loanId}/schedule")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public List<ScheduleResponse> schedule(@PathVariable UUID loanId) {
        return loanService.listSchedule(loanId);
    }

    /** Send overdue payment reminder to the borrower (owner/cashier only). */
    @PostMapping("/{loanId}/remind")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> sendReminder(@PathVariable UUID loanId) {
        loanService.sendOverdueReminder(loanId);
        return Map.of("message", "Reminder sent to borrower.");
    }

    /** Update a PENDING loan (amount and/or term). Owner only. */
    @PutMapping("/{loanId}")
    @PreAuthorize("hasRole('OWNER')")
    public LoanResponse update(@PathVariable UUID loanId, @Valid @RequestBody LoanUpdateRequest request) {
        return loanService.updateLoan(loanId, request);
    }

    /** Cancel (delete) a PENDING loan. Owner only. */
    @DeleteMapping("/{loanId}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID loanId) {
        loanService.cancelLoan(loanId);
    }
}
