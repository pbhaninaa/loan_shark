package com.loanshark.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestNotification {

    private String clientName;
    private String requestId;
    private String message;
}