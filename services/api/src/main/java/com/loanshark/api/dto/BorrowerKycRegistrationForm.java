package com.loanshark.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class BorrowerKycRegistrationForm {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    // SA ID must be 13 digits
    @NotBlank(message = "ID number is required")
    @Pattern(regexp = "\\d{13}", message = "ID number must be 13 digits")
    private String idNumber;

    // SA phone validation
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+27|0)[6-8][0-9]{8}$",
            message = "Invalid South African phone number")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Employment status is required")
    private String employmentStatus;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.00", message = "Income must be greater than or equal to 0")
    private BigDecimal monthlyIncome;

    private String employerName;

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @NotBlank(message = "Location name is required")
    private String locationName;

    // Documents
    @NotNull(message = "ID document is required")
    private MultipartFile idDocument;

    @NotNull(message = "Selfie image is required")
    private MultipartFile selfieImage;
}