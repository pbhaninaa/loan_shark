package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.LenderContactResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/lender-contact")
public class LenderContactController {

    @Value("${app.lender.name:Loan Shark Lending}")
    private String lenderName;

    @Value("${app.lender.phone:}")
    private String lenderPhone;

    @Value("${app.lender.email:}")
    private String lenderEmail;

    @Value("${app.lender.address:}")
    private String lenderAddress;

    @GetMapping
    public LenderContactResponse get() {
        return new LenderContactResponse(
            lenderName != null ? lenderName : "",
            lenderPhone != null ? lenderPhone : "",
            lenderEmail != null ? lenderEmail : "",
            lenderAddress != null ? lenderAddress : ""
        );
    }
}
