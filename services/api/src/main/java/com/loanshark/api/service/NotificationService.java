package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.NotificationResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.Notification;
import com.loanshark.api.entity.User;
import com.loanshark.api.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final EmailNotificationService emailNotificationService;

    public NotificationService(
        NotificationRepository notificationRepository,
        CurrentUserService currentUserService,
        EmailNotificationService emailNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> myNotifications(String query, int page, int size) {
        User currentUser = currentUserService.requireCurrentUser();
        Page<Notification> notificationPage = notificationRepository.searchUnreadByUserId(
            currentUser.getId(),
            "READ",
            query == null ? "" : query.trim(),
            PageRequest.of(page, size)
        );
        return new PageResponse<>(
            notificationPage.getContent().stream().map(this::toResponse).toList(),
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages()
        );
    }

    @Transactional
    public Notification notifyUser(UUID userId, String channel, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setChannel(channel);
        notification.setMessage(message);
        notification.setStatus("PENDING");
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        User currentUser = currentUserService.requireCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You can only update your own notifications");
        }
        notification.setStatus("READ");
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyBorrowerProfileCreated(Borrower borrower, boolean imageVerificationSkipped) {
        if (borrower.getUser() == null) {
            return;
        }
        String message = imageVerificationSkipped
            ? "Your borrower profile was created by staff and your account is active."
            : "Your borrower profile was created successfully and is now available in the system.";
        notifyUser(borrower.getUser().getId(), "PROFILE", message);
        emailNotificationService.send(
            borrower.getEmail(),
            "Loan Shark profile created",
            message
        );
    }

    @Transactional
    public void notifyBorrowerStatusChanged(Borrower borrower) {
        if (borrower.getUser() == null) {
            return;
        }
        String message = "Your borrower profile status changed to " + borrower.getStatus().name() + ".";
        notifyUser(borrower.getUser().getId(), "PROFILE_STATUS", message);
        emailNotificationService.send(
            borrower.getEmail(),
            "Loan Shark profile status changed",
            message
        );
    }

    @Transactional
    public void notifyLoanStatusChanged(Loan loan, String message) {
        Borrower borrower = loan.getBorrower();
        if (borrower.getUser() == null) {
            return;
        }
        notifyUser(borrower.getUser().getId(), "LOAN_STATUS", message);
        emailNotificationService.send(
            borrower.getEmail(),
            "Loan Shark application update",
            message
        );
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getChannel(),
            notification.getMessage(),
            notification.getStatus(),
            notification.getCreatedAt()
        );
    }
}
