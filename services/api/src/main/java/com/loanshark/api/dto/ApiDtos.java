package com.loanshark.api.dto;

import com.loanshark.api.entity.BorrowerStatus;
import com.loanshark.api.entity.DocumentType;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.PaymentMethod;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.RiskBand;
import com.loanshark.api.entity.ScheduleStatus;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.entity.UserStatus;
import com.loanshark.api.entity.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record AuthRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {
    }

    public record BorrowerRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String idNumber,
        @NotBlank String phone,
        String email,
        @NotBlank String address,
        @NotBlank String employmentStatus,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyIncome,
        String employerName
    ) {
    }

    public record OwnerRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {
    }

    public record StaffRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull UserRole role
    ) {
    }

    public record AuthResponse(
        String token,
        UUID userId,
        String username,
        UserRole role,
        UUID borrowerId
    ) {
    }

    public record SetupStatusResponse(
        boolean ownerExists
    ) {
    }

    public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
    ) {
    }

    public record ResetPasswordRequest(
        @NotBlank String newPassword
    ) {
    }

    /** Owner resets another user's password by user id. */
    public record ResetUserPasswordRequest(
        @NotNull UUID userId,
        @NotBlank String newPassword
    ) {
    }

    public record ForgotPasswordRequest(@NotBlank String username) {}

    public record ForgotPasswordResponse(
        String message,
        String resetLink
    ) {}

    /** Reset password using token from forgot-password flow (no auth). */
    public record ResetPasswordWithTokenRequest(
        @NotBlank String token,
        @NotBlank String newPassword
    ) {}

    public record BorrowerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String idNumber,
        @NotBlank String phone,
        String email,
        @NotBlank String address,
        @NotBlank String employmentStatus,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyIncome,
        String employerName,
        String username,
        String password
    ) {
    }

    public record BorrowerResponse(
        UUID id,
        String firstName,
        String lastName,
        String idNumber,
        String phone,
        String email,
        String address,
        String employmentStatus,
        BigDecimal monthlyIncome,
        String employerName,
        BorrowerStatus status,
        Integer riskScore
    ) {
    }

    public record BorrowerStatusUpdateRequest(
        @NotNull BorrowerStatus status
    ) {
    }

    public record BorrowerDocumentRequest(
        @NotNull DocumentType documentType,
        @NotBlank String fileUrl
    ) {
    }

    public record BorrowerDocumentResponse(
        UUID id,
        DocumentType documentType,
        String fileUrl,
        String originalFileName,
        String contentType,
        Long fileSizeBytes,
        Instant uploadedAt
    ) {
    }

    public record NotificationResponse(
        UUID id,
        String channel,
        String message,
        String status,
        Instant createdAt
    ) {
    }

    public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record VerificationResponse(
        UUID id,
        UUID borrowerId,
        VerificationStatus status,
        UUID idDocumentId,
        String idDocumentName,
        String idDocumentContentType,
        UUID selfieDocumentId,
        String selfieDocumentName,
        String selfieDocumentContentType,
        boolean saIdValid,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant locationCapturedAt,
        String locationName,
        String extractedFirstName,
        String extractedLastName,
        String extractedIdNumber,
        BigDecimal ocrConfidence,
        boolean detailsMatched,
        BigDecimal faceMatchScore,
        boolean faceMatched,
        String reviewNotes,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record VerificationReviewRequest(
        @NotBlank String notes
    ) {
    }

    /** Interest rate and period come from admin settings. Borrower only specifies amount; term comes from admin default (repayments reduce balance, interest continues per business rules). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanApplicationRequest(
        @NotNull UUID borrowerId,
        @NotNull @DecimalMin("1.00") BigDecimal loanAmount,
        @Positive Integer loanTermDays
    ) {
        /** Term is optional; when null, server uses admin default_loan_term_days. */
    }

    public record LoanDecisionRequest(
        @NotNull UUID loanId,
        String note
    ) {
    }

    public record LoanResponse(
        UUID id,
        UUID borrowerId,
        BigDecimal loanAmount,
        BigDecimal interestRate,
        BigDecimal totalAmount,
        Integer loanTermDays,
        LocalDate issueDate,
        LocalDate dueDate,
        LoanStatus status,
        Integer riskScore,
        RiskBand riskBand,
        InterestType interestType,
        Integer interestPeriodDays,
        Integer gracePeriodDays
    ) {
    }

    public record LoanInterestSettingsResponse(
        BigDecimal defaultInterestRate,
        InterestType interestType,
        Integer interestPeriodDays,
        Integer gracePeriodDays,
        Integer defaultLoanTermDays,
        Instant updatedAt
    ) {
    }

    public record LoanInterestSettingsUpdateRequest(
        @NotNull @DecimalMin("0.01") BigDecimal defaultInterestRate,
        @NotNull InterestType interestType,
        @NotNull @Positive Integer interestPeriodDays,
        @NotNull @jakarta.validation.constraints.Min(0) Integer gracePeriodDays,
        @NotNull @Positive Integer defaultLoanTermDays
    ) {
    }

    public record BusinessCapitalResponse(
        BigDecimal balance,
        BigDecimal totalMoneyOut,
        BigDecimal totalMoneyIn,
        BigDecimal expectedAmount
    ) {}

    public record BusinessCapitalTopUpRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record RepaymentRequest(
        @NotNull UUID loanId,
        @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String referenceNumber
    ) {
    }

    public record RepaymentResponse(
        UUID id,
        UUID loanId,
        BigDecimal amountPaid,
        Instant paymentDate,
        PaymentMethod paymentMethod,
        String referenceNumber,
        String capturedByUsername
    ) {
    }

    public record ScheduleResponse(
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amountDue,
        ScheduleStatus status
    ) {
    }

    public record RiskCheckRequest(
        @NotNull UUID borrowerId,
        @NotNull @DecimalMin("1.00") BigDecimal requestedAmount
    ) {
    }

    public record RiskCheckResponse(
        Integer score,
        RiskBand band,
        List<String> factors
    ) {
    }

    public record BlacklistRequest(
        @NotNull UUID borrowerId,
        @NotBlank String reason
    ) {
    }

    public record BlacklistResponse(
        UUID id,
        UUID borrowerId,
        String reason,
        Instant blacklistedAt
    ) {
    }

    public record UserRequest(
        @NotBlank String username,
        String password,
        @NotNull UserRole role,
        @NotNull UserStatus status
    ) {
    }

    public record UserResponse(
        UUID id,
        String username,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        UUID borrowerId
    ) {
    }

    public record DashboardSummaryResponse(
        long borrowers,
        long pendingLoans,
        long activeLoans,
        long overdueSchedules,
        long pendingVerifications,
        BigDecimal principalOutstanding,
        BigDecimal repaymentsCaptured
    ) {
    }

    public record ActionResponse(
        String category,
        String action,
        String entity,
        String entityId,
        UUID loanId,
        BigDecimal amount,
        String referenceNumber,
        String performedBy,
        String authorizedBy,
        String details,
        Instant timestamp
    ) {
    }

    /** Lender contact details for client help / enquiries. */
    public record LenderContactResponse(
        String name,
        String phone,
        String email,
        String address
    ) {
    }
}
