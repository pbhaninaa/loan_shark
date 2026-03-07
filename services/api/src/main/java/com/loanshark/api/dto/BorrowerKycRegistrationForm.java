package com.loanshark.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class BorrowerKycRegistrationForm {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String idNumber;

    @NotBlank
    private String phone;

    private String email;

    @NotBlank
    private String address;

    @NotBlank
    private String employmentStatus;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal monthlyIncome;

    private String employerName;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    @NotBlank
    private String locationName;

    @NotNull
    private MultipartFile idDocument;

    @NotNull
    private MultipartFile selfieImage;
}
