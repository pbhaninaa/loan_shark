package com.loanshark.api.dto;

import java.util.UUID;

public class RequestNotificationRequest {

    private UUID requestId;
    private String clientName;

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}