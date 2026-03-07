package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.LoanApplicationRequest;
import com.loanshark.api.dto.ApiDtos.LoanDecisionRequest;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.LoanResponse;
import com.loanshark.api.dto.ApiDtos.ScheduleResponse;
import com.loanshark.api.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
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
    public LoanResponse get(@PathVariable Long loanId) {
        return loanService.getLoan(loanId);
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('OWNER')")
    public LoanResponse approve(@Valid @RequestBody LoanDecisionRequest request) {
        return loanService.approve(request);
    }

    @PostMapping("/reject")
    @PreAuthorize("hasRole('OWNER')")
    public LoanResponse reject(@Valid @RequestBody LoanDecisionRequest request) {
        return loanService.reject(request);
    }

    @GetMapping("/{loanId}/schedule")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public List<ScheduleResponse> schedule(@PathVariable Long loanId) {
        return loanService.listSchedule(loanId);
    }
}
