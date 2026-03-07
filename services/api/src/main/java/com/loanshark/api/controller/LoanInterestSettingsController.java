package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsUpdateRequest;
import com.loanshark.api.service.LoanInterestSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/loan-interest")
public class LoanInterestSettingsController {

    private final LoanInterestSettingsService loanInterestSettingsService;

    public LoanInterestSettingsController(LoanInterestSettingsService loanInterestSettingsService) {
        this.loanInterestSettingsService = loanInterestSettingsService;
    }

    @GetMapping
    public LoanInterestSettingsResponse get() {
        return loanInterestSettingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public LoanInterestSettingsResponse update(@Valid @RequestBody LoanInterestSettingsUpdateRequest request) {
        return loanInterestSettingsService.update(request);
    }
}
