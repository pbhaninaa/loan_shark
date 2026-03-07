package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.BusinessCapitalResponse;
import com.loanshark.api.dto.ApiDtos.BusinessCapitalTopUpRequest;
import com.loanshark.api.service.BusinessCapitalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/settings/business-capital")
public class BusinessCapitalController {

    private final BusinessCapitalService businessCapitalService;

    public BusinessCapitalController(BusinessCapitalService businessCapitalService) {
        this.businessCapitalService = businessCapitalService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BusinessCapitalResponse get() {
        return businessCapitalService.getSummary();
    }

    @PostMapping("/top-up")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void topUp(@Valid @RequestBody BusinessCapitalTopUpRequest request) {
        businessCapitalService.addFunds(request.amount());
    }
}
