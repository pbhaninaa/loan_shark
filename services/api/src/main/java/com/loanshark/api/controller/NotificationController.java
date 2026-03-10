package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.NotificationResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.RequestNotification;
import com.loanshark.api.dto.RequestNotificationRequest;
import com.loanshark.api.service.NotificationService;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/request")
    public String createRequest(@RequestBody RequestNotificationRequest request) {

        RequestNotification notification = new RequestNotification();
        notification.setClientName(request.getClientName());
        notification.setRequestId(request.getRequestId().toString());
        notification.setMessage("New request received");

        notificationService.notifyProvider(notification);

        return "Request sent";
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER','BORROWER')")
    public PageResponse<NotificationResponse> myNotifications(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return notificationService.myNotifications(q, page, size);
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('OWNER','CASHIER','BORROWER')")
    public void markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
    }
}