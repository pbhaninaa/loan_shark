package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos;
import com.loanshark.api.dto.ApiDtos.NotificationResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.RequestNotification;
import com.loanshark.api.entity.*;
import com.loanshark.api.repository.NotificationRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final EmailNotificationService emailNotificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BusinessContactService businessContactService;

    public NotificationService(
            NotificationRepository notificationRepository,
            CurrentUserService currentUserService,
            EmailNotificationService emailNotificationService,
            SimpMessagingTemplate messagingTemplate,
            BusinessContactService businessContactService
    ) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.emailNotificationService = emailNotificationService;
        this.messagingTemplate = messagingTemplate;
        this.businessContactService = businessContactService;
    }

    // ------------------- Messaging -------------------
    public void notifyProvider(RequestNotification notification) {
        messagingTemplate.convertAndSend(
                "/topic/provider-requests",
                notification
        );
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

    // ------------------- Dynamic Company Signature -------------------
    private String getCompanySignature() {
        ApiDtos.LenderContactResponse contact = businessContactService.get();
        StringBuilder signature = new StringBuilder("\n\n--\nBest regards,\nThe Loan Shark Team");
        if (!contact.name().isBlank()) signature.append("\n").append(contact.name());
        if (!contact.phone().isBlank()) signature.append("\nPhone: ").append(contact.phone());
        if (!contact.email().isBlank()) signature.append("\nEmail: ").append(contact.email());
        if (!contact.address().isBlank()) signature.append("\nAddress: ").append(contact.address());
        return signature.toString();
    }

    // ------------------- Notifications -------------------
    @Transactional
    public void notifyBorrowerProfileCreated(Borrower borrower, boolean imageVerificationSkipped) {
        if (borrower.getUser() == null) return;
        String message = imageVerificationSkipped
                ? "Your profile was created by our staff, and your account is active."
                : "Your Client profile has been successfully created and is now active in our system.";
        notifyUser(borrower.getUser().getId(), "PROFILE", message);

        String emailBody = "Hello,\n\n" +
                (imageVerificationSkipped
                        ? "Your profile was created by our staff, and your account is active.\n\n"
                        : "Your Client profile has been successfully created and is now active in our system.\n\n") +
                "Thank you for choosing Loan Shark. If you have any questions, feel free to contact us." +
                getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Your Loan Shark Profile Has Been Created",
                emailBody
        );
    }
    @Transactional
    public void notifyPaymentDueReminder(RepaymentSchedule schedule) {
        Borrower borrower = schedule.getLoan().getBorrower();
        if (borrower.getUser() == null) return;

        Loan loan = schedule.getLoan();

        // In-app notification
        String message = "Reminder: Your loan installment of amount " + schedule.getAmountDue()
                + " for Loan ID " + loan.getId() + " is due on " + schedule.getDueDate() + ".";
        notifyUser(borrower.getUser().getId(), "PAYMENT_DUE_REMINDER", message);

        // Email notification
        String emailBody = "Hello " + borrower.getFirstName() + ",\n\n" +
                "This is a friendly reminder that your loan installment for Loan ID **" + loan.getId() + "** " +
                "amounting to **" + schedule.getAmountDue() + "** is due on **" + schedule.getDueDate() + "**.\n\n" +
                "Please ensure the payment is made by the due date to avoid any late fees.\n\n" +
                "If you have already paid or have questions regarding your payment, feel free to contact us.\n\n" +
                getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Reminder: Upcoming Loan Payment Due",
                emailBody
        );
    }
    @Transactional
    public void notifyBorrowerProfileUpdated(Borrower borrower) {
        if (borrower.getUser() == null) return;
        String message = "Your profile was updated. Check your details in My Profile.";
        notifyUser(borrower.getUser().getId(), "PROFILE", message);

        String emailBody = "Hello,\n\n" +
                "Your Client profile details were recently updated. Please review your information in your My Profile section to ensure everything is correct.\n\n" +
                "If you did not make these changes, please contact our support team immediately." +
                getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Your Loan Shark Profile Has Been Updated",
                emailBody
        );
    }

    @Transactional
    public void notifyBorrowerStatusChanged(Borrower borrower) {
        if (borrower.getUser() == null) return;
        String statusName = borrower.getStatus() != null ? borrower.getStatus().name() : "UNKNOWN";
        String message = "Your Client profile status changed to " + statusName + ".";
        notifyUser(borrower.getUser().getId(), "PROFILE_STATUS", message);

        String emailBody = "Hello,\n\n" +
                "We want to inform you that the status of your Client profile has changed to: **" + statusName + "**.\n\n" +
                "If you have any questions about this change, please get in touch with us." +
                getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Update: Your Client Profile Status Has Changed",
                emailBody
        );
    }

    @Transactional
    public void notifyBorrowerProfileDeleted(Borrower borrower, String reason) {
        if (borrower.getUser() == null) return;
        String message = "Your Client profile has been removed from the system."
                + (reason != null && !reason.isBlank() ? " " + reason : "");
        notifyUser(borrower.getUser().getId(), "PROFILE", message);

        String emailBody = "Hello,\n\n" +
                "Your Client profile has been removed from our system.\n\n" +
                (reason != null && !reason.isBlank() ? "Reason: " + reason + "\n\n" : "") +
                "If you believe this was a mistake or have any concerns, please contact our support team." +
                getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Your Client Profile Has Been Removed",
                emailBody
        );
    }
    @Transactional
    public void notifyBorrowerLoanOverdue(Loan loan, Borrower borrower) {
        if (borrower.getUser() == null) return;

        // Construct the message for in-app notification
        String message = "Your loan with ID " + loan.getId() + " is overdue. " +
                "Please take action to avoid penalties.";
        notifyUser(borrower.getUser().getId(), "LOAN_OVERDUE", message);

        // Construct the email body
        String emailBody = "Hello " + borrower.getFirstName() + ",\n\n" +
                "We want to inform you that your loan with ID **" + loan.getId() + "** " +
                "is overdue as of " + loan.getDueDate() + ".\n" +
                "Please make the necessary payment as soon as possible to avoid additional charges.\n\n" +
                "If you have already made the payment or have any questions, please contact us.\n\n" +
                getCompanySignature();

        // Send email
        emailNotificationService.send(
                borrower.getEmail(),
                "Urgent: Your Loan is Overdue",
                emailBody
        );
    }
    @Transactional
    public void notifyLoanApproved(Loan loan) {
        Borrower borrower = loan.getBorrower();
        if (borrower.getUser() == null) return;
        String message = String.format(
                "Your loan was APPROVED. Loan #%s: Amount R%s, Interest %s%% (%s), Term %s days. Total to repay: R%s. Due date: %s. You can pay in full or in installments.",
                loan.getId(),
                loan.getLoanAmount() != null ? loan.getLoanAmount().toPlainString() : "0",
                loan.getInterestRate() != null ? loan.getInterestRate().toPlainString() : "0",
                loan.getInterestType() != null ? loan.getInterestType().name() : "SIMPLE",
                loan.getLoanTermDays() != null ? loan.getLoanTermDays() : "0",
                loan.getTotalAmount() != null ? loan.getTotalAmount().toPlainString() : "0",
                loan.getDueDate() != null ? loan.getDueDate().toString() : "—"
        );
        notifyUser(borrower.getUser().getId(), "LOAN_STATUS", message);

        String emailBody = String.format(
                "Hello,\n\nGood news! Your loan application (Loan # %s) has been approved with the following details:\n\n" +
                        "- Amount: R%s\n- Interest Rate: %s%% (%s)\n- Term: %s days\n- Total Amount to Repay: R%s\n- Due Date: %s\n\n" +
                        "You can choose to pay the loan in full or in installments.\n\nThank you for trusting Loan Shark.",
                loan.getId(),
                loan.getLoanAmount() != null ? loan.getLoanAmount().toPlainString() : "0",
                loan.getInterestRate() != null ? loan.getInterestRate().toPlainString() : "0",
                loan.getInterestType() != null ? loan.getInterestType().name() : "SIMPLE",
                loan.getLoanTermDays() != null ? loan.getLoanTermDays() : "0",
                loan.getTotalAmount() != null ? loan.getTotalAmount().toPlainString() : "0",
                loan.getDueDate() != null ? loan.getDueDate().toString() : "—"
        ) + getCompanySignature();

        emailNotificationService.send(
                borrower.getEmail(),
                "Your Loan Has Been Approved",
                emailBody
        );
    }

    // ------------------- Other notifications (Loan Rejected, Payment Reminder, Overdue, Loan Status Changed) -------------------
    // Append getCompanySignature() in the same way as above
    // Similar to notifyLoanRejected(), notifyPaymentDueReminder(), notifyOverdueReminder(), notifyLoanStatusChanged()

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