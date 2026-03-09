package com.loanshark.api.service;

import com.loanshark.api.entity.BusinessCapital;
import com.loanshark.api.entity.UuidConstants;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.AuditLogRepository;
import com.loanshark.api.repository.BlacklistEntryRepository;
import com.loanshark.api.repository.BorrowerVerificationRepository;
import com.loanshark.api.repository.BusinessCapitalRepository;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.NotificationRepository;
import com.loanshark.api.repository.PasswordResetTokenRepository;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import com.loanshark.api.repository.RiskAssessmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Resets the database so that only users, clients (borrowers), and their profiles remain.
 * Removes all history: loans, repayments, schedules, blacklist, notifications, audit logs,
 * cash transactions, risk assessments, password reset tokens, borrower verifications.
 * Resets business capital to zero. Owner only.
 */
@Service
public class DatabaseResetService {

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final RepaymentRepository repaymentRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final LoanRepository loanRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BorrowerVerificationRepository borrowerVerificationRepository;
    private final BusinessCapitalRepository businessCapitalRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public DatabaseResetService(
        RepaymentScheduleRepository repaymentScheduleRepository,
        RepaymentRepository repaymentRepository,
        CashTransactionRepository cashTransactionRepository,
        RiskAssessmentRepository riskAssessmentRepository,
        LoanRepository loanRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        NotificationRepository notificationRepository,
        AuditLogRepository auditLogRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        BorrowerVerificationRepository borrowerVerificationRepository,
        BusinessCapitalRepository businessCapitalRepository,
        CurrentUserService currentUserService,
        AuditLogService auditLogService
    ) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.repaymentRepository = repaymentRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.loanRepository = loanRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.borrowerVerificationRepository = borrowerVerificationRepository;
        this.businessCapitalRepository = businessCapitalRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    /**
     * Removes all history; keeps users, borrowers, and borrower_documents.
     * Resets business capital to zero. Owner only.
     */
    @Transactional
    public void resetHistory() {
        if (currentUserService.requireCurrentUser().getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only the owner can reset history");
        }

        // Delete in FK-safe order (children before parents)
        repaymentScheduleRepository.deleteAll();
        repaymentRepository.deleteAll();
        cashTransactionRepository.deleteAll();
        riskAssessmentRepository.deleteAll();
        loanRepository.deleteAll();
        blacklistEntryRepository.deleteAll();
        notificationRepository.deleteAll();
        auditLogRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        borrowerVerificationRepository.deleteAll();

        // Reset business capital (keep the row, zero the amounts)
        businessCapitalRepository.findByIdForUpdate(UuidConstants.BUSINESS_CAPITAL_ID).ifPresent(cap -> {
            cap.setBalance(BigDecimal.ZERO);
            cap.setTotalOwnerAdded(BigDecimal.ZERO);
            cap.setUpdatedAt(Instant.now());
            businessCapitalRepository.save(cap);
        });

        auditLogService.record(
            currentUserService.requireCurrentUser().getId(),
            "RESET_HISTORY",
            "Database",
            "reset",
            "History cleared. Users, clients and their profiles kept; business capital reset to zero."
        );
    }
}
