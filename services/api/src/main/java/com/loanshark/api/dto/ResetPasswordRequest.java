package com.loanshark.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for resetting a user's password (by owner/admin). */
public record ResetPasswordRequest(
    @NotBlank String newPassword
) {}
