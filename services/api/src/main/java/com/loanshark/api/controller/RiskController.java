package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.RiskCheckRequest;
import com.loanshark.api.dto.ApiDtos.RiskCheckResponse;
import com.loanshark.api.service.BorrowerService;
import com.loanshark.api.service.RiskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
public class RiskController {

    private final BorrowerService borrowerService;
    private final RiskService riskService;

    public RiskController(BorrowerService borrowerService, RiskService riskService) {
        this.borrowerService = borrowerService;
        this.riskService = riskService;
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public RiskCheckResponse check(@Valid @RequestBody RiskCheckRequest request) {
        return riskService.assess(
            borrowerService.findBorrower(request.borrowerId()),
            request.requestedAmount()
        );
    }
}
