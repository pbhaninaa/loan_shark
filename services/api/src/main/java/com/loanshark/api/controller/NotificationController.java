package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.NotificationResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public PageResponse<NotificationResponse> myNotifications(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return notificationService.myNotifications(q, page, size);
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'BORROWER')")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }
}
