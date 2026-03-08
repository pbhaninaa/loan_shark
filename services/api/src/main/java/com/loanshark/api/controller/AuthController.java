package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.AuthRequest;
import com.loanshark.api.dto.ApiDtos.AuthResponse;
import com.loanshark.api.dto.ApiDtos.ChangePasswordRequest;
import com.loanshark.api.dto.ApiDtos.ForgotPasswordRequest;
import com.loanshark.api.dto.ApiDtos.ForgotPasswordResponse;
import com.loanshark.api.dto.ApiDtos.OwnerRegistrationRequest;
import com.loanshark.api.dto.ResetPasswordWithTokenRequest;
import com.loanshark.api.dto.ApiDtos.ResetUserPasswordRequest;
import com.loanshark.api.dto.ApiDtos.SetupStatusResponse;
import com.loanshark.api.dto.ApiDtos.StaffRegistrationRequest;
import com.loanshark.api.dto.BorrowerKycRegistrationForm;
import com.loanshark.api.dto.ApiDtos.BusinessCapitalResponse;
import com.loanshark.api.dto.ApiDtos.BusinessCapitalTopUpRequest;
import com.loanshark.api.service.AuthService;
import com.loanshark.api.service.BorrowerVerificationService;
import com.loanshark.api.service.BusinessCapitalService;
import com.loanshark.api.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final BorrowerVerificationService borrowerVerificationService;

    private final UserManagementService userManagementService;
    private final BusinessCapitalService businessCapitalService;

    public AuthController(AuthService authService, BorrowerVerificationService borrowerVerificationService, UserManagementService userManagementService, BusinessCapitalService businessCapitalService) {
        this.authService = authService;
        this.borrowerVerificationService = borrowerVerificationService;
        this.userManagementService = userManagementService;
        this.businessCapitalService = businessCapitalService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    @PostMapping("/reset-user-password")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetUserPassword(@Valid @RequestBody ResetUserPasswordRequest request) {
        userManagementService.resetPassword(request.userId(), new com.loanshark.api.dto.ResetPasswordRequest(request.newPassword()));
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPasswordWithToken(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        authService.resetPasswordWithToken(request);
    }

    @GetMapping("/setup-status")
    public SetupStatusResponse setupStatus() {
        return authService.setupStatus();
    }

    @GetMapping("/business-capital")
    @PreAuthorize("isAuthenticated()")
    public BusinessCapitalResponse getBusinessCapital() {
        return businessCapitalService.getSummary();
    }

    @PostMapping("/business-capital/top-up")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void topUpBusinessCapital(@Valid @RequestBody BusinessCapitalTopUpRequest request) {
        businessCapitalService.addFunds(request.amount());
    }

    @PostMapping("/register/owner")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerOwner(@Valid @RequestBody OwnerRegistrationRequest request) {
        return authService.registerOwner(request);
    }

    @PostMapping("/register/staff")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerStaff(@Valid @RequestBody StaffRegistrationRequest request) {
        return authService.registerStaff(request);
    }

    @PostMapping(value = "/register/borrower", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerBorrower(@Valid @ModelAttribute BorrowerKycRegistrationForm request) {
        return borrowerVerificationService.registerBorrower(request);
    }
}
