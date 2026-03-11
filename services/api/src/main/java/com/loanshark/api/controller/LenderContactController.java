package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.BusinessContactUpdateRequest;
import com.loanshark.api.dto.ApiDtos.LenderContactResponse;
import com.loanshark.api.service.BusinessContactService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings/lender-contact")
public class LenderContactController {

    private final BusinessContactService businessContactService;

    public LenderContactController(BusinessContactService businessContactService) {
        this.businessContactService = businessContactService;
    }

    @GetMapping
    public LenderContactResponse get() {
        return businessContactService.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public LenderContactResponse update(@Valid @RequestBody BusinessContactUpdateRequest request) {
        return businessContactService.createOrUpdate(request);
    }
}
