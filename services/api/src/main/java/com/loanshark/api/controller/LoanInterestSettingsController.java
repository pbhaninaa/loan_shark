package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.ExpectedAmountAtEndOfTermResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsUpdateRequest;
import com.loanshark.api.service.BusinessCapitalService;
import com.loanshark.api.service.LoanInterestSettingsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/loan-interest")
public class LoanInterestSettingsController {

    private final LoanInterestSettingsService loanInterestSettingsService;
    private final BusinessCapitalService businessCapitalService;

    public LoanInterestSettingsController(
        LoanInterestSettingsService loanInterestSettingsService,
        BusinessCapitalService businessCapitalService
    ) {
        this.loanInterestSettingsService = loanInterestSettingsService;
        this.businessCapitalService = businessCapitalService;
    }

    @GetMapping
    public LoanInterestSettingsResponse get() {
        return loanInterestSettingsService.get();
    }

    /**
     * Expected amount due at end of default loan term for a given principal using current settings.
     * If principal is omitted and caller is owner, uses current business capital balance (e.g. after top-up).
     */
    @GetMapping("/expected-amount")
    public ExpectedAmountAtEndOfTermResponse getExpectedAmountAtEndOfTerm(
        @RequestParam(required = false) BigDecimal principal
    ) {
        if (principal == null) {
            principal = businessCapitalService.getBalance();
        }
        return loanInterestSettingsService.getExpectedAmountAtEndOfTerm(principal);
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public LoanInterestSettingsResponse update(@Valid @RequestBody LoanInterestSettingsUpdateRequest request) {
        return loanInterestSettingsService.update(request);
    }
}
