package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.ActionResponse;
import com.loanshark.api.dto.ApiDtos.DashboardSummaryResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.entity.AuditLog;
import com.loanshark.api.entity.CashTransaction;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.ScheduleStatus;
import com.loanshark.api.entity.VerificationStatus;
import com.loanshark.api.repository.AuditLogRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.BorrowerVerificationRepository;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.NotificationRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import com.loanshark.api.repository.UserRepository;
import com.loanshark.api.service.CurrentUserService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final AuditLogRepository auditLogRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final UserRepository userRepository;
    private final BorrowerVerificationRepository borrowerVerificationRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(
        BorrowerRepository borrowerRepository,
        LoanRepository loanRepository,
        RepaymentRepository repaymentRepository,
        RepaymentScheduleRepository repaymentScheduleRepository,
        AuditLogRepository auditLogRepository,
        CashTransactionRepository cashTransactionRepository,
        UserRepository userRepository,
        BorrowerVerificationRepository borrowerVerificationRepository,
        NotificationRepository notificationRepository,
        CurrentUserService currentUserService
    ) {
        this.borrowerRepository = borrowerRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.auditLogRepository = auditLogRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.userRepository = userRepository;
        this.borrowerVerificationRepository = borrowerVerificationRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        BigDecimal principalOutstanding = loanRepository.findAll().stream()
            .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
            .map(loan -> loan.getLoanAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal repaymentsCaptured = repaymentRepository.findAll().stream()
            .map(repayment -> repayment.getAmountPaid())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingVerifications = borrowerVerificationRepository.countByStatus(VerificationStatus.MANUAL_REVIEW);
        long unreadNotifications = 0;
        try {
            unreadNotifications = notificationRepository.countByUserIdAndStatusNot(
                currentUserService.requireCurrentUser().getId(),
                "READ"
            );
        } catch (Exception ignored) {
            // no current user (e.g. public)
        }
        return new DashboardSummaryResponse(
            borrowerRepository.count(),
            loanRepository.findAllByStatusOrderByCreatedAtAsc(LoanStatus.PENDING).size(),
            loanRepository.findAllByStatusOrderByCreatedAtAsc(LoanStatus.ACTIVE).size(),
            repaymentScheduleRepository.countByStatus(ScheduleStatus.OVERDUE),
            pendingVerifications,
            unreadNotifications,
            principalOutstanding,
            repaymentsCaptured
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ActionResponse> recentActions(String query, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<AuditLog> auditLogs = normalizedQuery.isBlank()
            ? auditLogRepository.findTop50ByOrderByTimestampDesc()
            : auditLogRepository.searchTop200(normalizedQuery);
        List<CashTransaction> cashTransactions = normalizedQuery.isBlank()
            ? cashTransactionRepository.findTop50ByOrderByCapturedAtDesc()
            : cashTransactionRepository.searchTop200(normalizedQuery);

        Set<UUID> userIds = Stream.concat(
                auditLogs.stream().map(AuditLog::getUserId),
                cashTransactions.stream().flatMap(transaction -> Stream.of(
                    transaction.getCapturedBy() == null ? null : transaction.getCapturedBy().getId(),
                    transaction.getAuthorizedBy() == null ? null : transaction.getAuthorizedBy().getId()
                ))
            )
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        Map<UUID, String> usernames = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(user -> user.getId(), user -> user.getUsername()));

        List<ActionResponse> actions = Stream.concat(
                auditLogs.stream().map(log -> new ActionResponse(
                    "AUDIT",
                    log.getAction(),
                    log.getEntity(),
                    log.getEntityId(),
                    null,
                    null,
                    null,
                    log.getUserId() == null ? "System" : usernames.getOrDefault(log.getUserId(), "User #" + log.getUserId()),
                    null,
                    log.getDetails(),
                    log.getTimestamp()
                )),
                cashTransactions.stream().map(transaction -> new ActionResponse(
                    "TRANSACTION",
                    transaction.getType().name(),
                    "CashTransaction",
                    transaction.getId().toString(),
                    transaction.getLoan() == null ? null : transaction.getLoan().getId(),
                    transaction.getAmount(),
                    transaction.getReferenceNumber(),
                    transaction.getCapturedBy() == null ? "Unknown" : usernames.getOrDefault(transaction.getCapturedBy().getId(), "Unknown"),
                    transaction.getAuthorizedBy() == null ? "Unknown" : usernames.getOrDefault(transaction.getAuthorizedBy().getId(), "Unknown"),
                    transaction.getType().name() + " for loan #"
                        + (transaction.getLoan() == null ? "-" : transaction.getLoan().getId()),
                    transaction.getCapturedAt()
                ))
            )
            .sorted(Comparator.comparing(ActionResponse::timestamp).reversed())
            .toList();

        int fromIndex = Math.min(page * size, actions.size());
        int toIndex = Math.min(fromIndex + size, actions.size());
        List<ActionResponse> content = actions.subList(fromIndex, toIndex);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) actions.size() / size);
        return new PageResponse<>(content, page, size, actions.size(), totalPages);
    }
}
