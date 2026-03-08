package com.loanshark.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Reset password using token from forgot-password flow (no auth). */
public record ResetPasswordWithTokenRequest(
    @NotBlank String token,
    @NotBlank String newPassword
) {}
