package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.LoanApplicationRequest;
import com.loanshark.api.dto.ApiDtos.LoanDecisionRequest;
import com.loanshark.api.dto.ApiDtos.LoanResponse;
import com.loanshark.api.dto.ApiDtos.LoanUpdateRequest;
import com.loanshark.api.dto.ApiDtos.RiskCheckResponse;
import com.loanshark.api.dto.ApiDtos.ScheduleResponse;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.CashTransaction;
import com.loanshark.api.entity.CashTransactionType;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.LoanInterestSettings;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.RiskBand;
import com.loanshark.api.entity.ScheduleStatus;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.LoanInterestSettingsRepository;
import com.loanshark.api.service.BusinessCapitalService;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import com.loanshark.api.entity.UuidConstants;
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final BorrowerRepository borrowerRepository;
    private final BorrowerService borrowerService;
    private final RiskService riskService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final BorrowerVerificationService borrowerVerificationService;
    private final NotificationService notificationService;
    private final LoanInterestSettingsRepository loanInterestSettingsRepository;
    private final InterestCalculationService interestCalculationService;
    private final BusinessCapitalService businessCapitalService;
    private final RepaymentRepository repaymentRepository;

    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal("0.80");

    @Value("${app.loan.cashier-approval-limit:10000}")
    private String cashierApprovalLimitConfig;

    private BigDecimal cashierApprovalLimit;

    @PostConstruct
    void initCashierLimit() {
        this.cashierApprovalLimit = new BigDecimal(cashierApprovalLimitConfig != null ? cashierApprovalLimitConfig.trim() : "10000");
    }

    public LoanService(
            LoanRepository loanRepository,
            RepaymentScheduleRepository repaymentScheduleRepository,
            CashTransactionRepository cashTransactionRepository,
            BorrowerRepository borrowerRepository,
            BorrowerService borrowerService,
            RiskService riskService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            BorrowerVerificationService borrowerVerificationService,
            NotificationService notificationService,
            LoanInterestSettingsRepository loanInterestSettingsRepository,
            InterestCalculationService interestCalculationService,
            BusinessCapitalService businessCapitalService,
            RepaymentRepository repaymentRepository
    ) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.borrowerRepository = borrowerRepository;
        this.borrowerService = borrowerService;
        this.riskService = riskService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.borrowerVerificationService = borrowerVerificationService;
        this.notificationService = notificationService;
        this.loanInterestSettingsRepository = loanInterestSettingsRepository;
        this.interestCalculationService = interestCalculationService;
        this.businessCapitalService = businessCapitalService;
        this.repaymentRepository = repaymentRepository;
    }

    @Transactional
    public LoanResponse apply(LoanApplicationRequest request) {
        Borrower borrower = borrowerService.findBorrower(request.borrowerId());
        if (borrowerService.isBlacklisted(borrower.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Borrower is blacklisted");
        }

        List<Loan> activeLoans = loanRepository.findByBorrowerIdAndStatus(borrower.getId(), LoanStatus.ACTIVE);
        for (Loan active : activeLoans) {
            BigDecimal totalOwed = active.getTotalAmount();
            BigDecimal paid = repaymentRepository.sumAmountPaidByLoanId(active.getId());
            if (paid == null) paid = BigDecimal.ZERO;
            BigDecimal requiredMin = totalOwed.multiply(EIGHTY_PERCENT);
            if (paid.compareTo(requiredMin) < 0) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "You must pay at least 80% of your current loan (Loan #" + active.getId()
                                + ": " + paid + " of " + totalOwed + " paid) before applying for a new loan."
                );
            }
        }

        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID currentBorrowerId = borrowerRepository.findByUserId(currentUser.getId())
                    .map(Borrower::getId)
                    .orElseThrow();
            if (!currentBorrowerId.equals(request.borrowerId())) {
                throw new ResponseStatusException(FORBIDDEN, "Borrowers can only apply for themselves");
            }
        }

        BigDecimal available = businessCapitalService.getBalance();
        if (available.compareTo(request.loanAmount()) < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Insufficient funds to disburse this loan. Available: " + available + ". Required: " + request.loanAmount() + ". Please ask the admin to add funds before applying."
            );
        }

        LoanInterestSettings settings = loanInterestSettingsRepository.findById(com.loanshark.api.entity.UuidConstants.LOAN_INTEREST_SETTINGS_ID).orElse(null);
        if (settings == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Loan interest settings are not configured; contact the administrator.");
        }
        BigDecimal limitPct = settings.getBorrowerLimitPercentageSalaryBased() != null ? settings.getBorrowerLimitPercentageSalaryBased() : BigDecimal.valueOf(100);
        BigDecimal monthlyIncome = borrower.getMonthlyIncome() != null ? borrower.getMonthlyIncome() : BigDecimal.ZERO;
        BigDecimal maxAllowed = monthlyIncome.multiply(limitPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        if (request.loanAmount().compareTo(maxAllowed) > 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Loan amount exceeds the limit. Maximum allowed is " + limitPct + "% of monthly income (R" + maxAllowed + "). Your monthly income: R" + monthlyIncome + "."
            );
        }
        BigDecimal rate = settings.getDefaultInterestRate();
        InterestType interestType = settings.getInterestType();
        int periodDays = settings.getInterestPeriodDays() != null ? settings.getInterestPeriodDays() : 30;
        int gracePeriodDays = settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0;
        int defaultTerm = settings.getDefaultLoanTermDays() != null && settings.getDefaultLoanTermDays() > 0
                ? settings.getDefaultLoanTermDays()
                : 365;
        int termDays = request.loanTermDays() != null && request.loanTermDays() > 0
                ? request.loanTermDays()
                : defaultTerm;
        BigDecimal totalAmount = interestCalculationService.computeTotalAmount(
                request.loanAmount(),
                termDays,
                settings
        );

        Loan loan = new Loan();
        loan.setBorrower(borrower);
        loan.setLoanAmount(request.loanAmount());
        loan.setInterestRate(rate);
        loan.setInterestType(interestType);
        loan.setInterestPeriodDays(periodDays);
        loan.setGracePeriodDays(gracePeriodDays);
        loan.setTotalAmount(totalAmount);
        loan.setLoanTermDays(termDays);
        loan.setCreatedBy(currentUser);
        loan = loanRepository.save(loan);

        RiskCheckResponse result = riskService.assess(borrower, request.loanAmount());
        loan.setRiskScore(result.score());
        loan.setRiskBand(result.band());
        loanRepository.save(loan);
        riskService.persistAssessment(borrower, loan, result);

        auditLogService.record(currentUser.getId(), "APPLY_LOAN", "Loan", loan.getId().toString(), String.join("; ", result.factors()));
        notificationService.notifyBorrowerStatusChanged(borrower);
        return toResponse(loan);
    }

    // ------------------ LIST LOANS ------------------
    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listAll(String query, List<LoanStatus> statuses, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Loan> loanPage;

        if (statuses != null && !statuses.isEmpty()) {
            loanPage = loanRepository.searchByStatusIn(query == null ? "" : query.trim(), statuses, pageRequest);
        } else {
            loanPage = loanRepository.search(query == null ? "" : query.trim(), pageRequest);
        }

        return mapLoanPageWithRepayments(loanPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listMyLoans(String query, int page, int size) {
        User currentUser = currentUserService.requireCurrentUser();

        UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Borrower profile not found"));

        Page<Loan> loanPage = loanRepository.searchWithBorrowerAndUser(query == null ? "" : query.trim(), borrowerId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return mapLoanPageWithRepayments(loanPage);
    }

    private PageResponse<LoanResponse> mapLoanPageWithRepayments(Page<Loan> loanPage) {
        List<UUID> loanIds = loanPage.getContent().stream().map(Loan::getId).toList();
        Map<UUID, BigDecimal> amountPaidMap = loanIds.isEmpty() ? Map.of() : buildAmountPaidMap(loanIds);
        List<RepaymentSchedule> allSchedules = loanIds.isEmpty() ? List.of()
                : repaymentScheduleRepository.findByLoanIdsOrderByInstallmentNumber(loanIds);

        Map<UUID, List<RepaymentSchedule>> schedulesByLoanId = allSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getLoan().getId()));

        List<LoanResponse> loanResponses = loanPage.getContent().stream()
                .map(loan -> toResponse(loan, amountPaidMap.getOrDefault(loan.getId(), BigDecimal.ZERO),
                        schedulesByLoanId.getOrDefault(loan.getId(), List.of())))
                .toList();

        return new PageResponse<>(loanResponses, loanPage.getNumber(), loanPage.getSize(),
                loanPage.getTotalElements(), loanPage.getTotalPages());
    }

    // ------------------ GET SINGLE LOAN ------------------
    @Transactional(readOnly = true)
    public LoanResponse getLoan(UUID loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);

        BigDecimal amountPaid = repaymentRepository.sumAmountPaidByLoanId(loan.getId());
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loan.getId());

        return toResponse(loan, amountPaid != null ? amountPaid : BigDecimal.ZERO, schedules);
    }

    // ------------------ APPROVE / REJECT / UPDATE / CANCEL ------------------
    @Transactional
    public LoanResponse approve(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUser.getRole() != UserRole.OWNER
                && !(currentUser.getRole() == UserRole.CASHIER && loan.getLoanAmount().compareTo(cashierApprovalLimit) < 0)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Only owner can approve loans over " + cashierApprovalLimit + "; cashiers may approve loans under that amount.");
        }

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be approved");
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(loan.getLoanTermDays()));
        loan.setApprovedBy(currentUser);
        loanRepository.save(loan);

        generateSchedule(loan);
        businessCapitalService.deductForDisbursement(loan.getLoanAmount());
        createCashDisbursement(loan);

        auditLogService.record(currentUser.getId(), "APPROVE_LOAN", "Loan", loan.getId().toString(),
                request.note() == null ? "" : request.note());

        notificationService.notifyLoanApproved(loan);

        return getLoan(loan.getId());
    }

    @Transactional
    public LoanResponse reject(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUser.getRole() != UserRole.OWNER
                && !(currentUser.getRole() == UserRole.CASHIER && loan.getLoanAmount().compareTo(cashierApprovalLimit) < 0)) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Only owner can reject loans over " + cashierApprovalLimit + "; cashiers may reject loans under that amount.");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(currentUser);
        loanRepository.save(loan);

        auditLogService.record(currentUser.getId(), "REJECT_LOAN", "Loan", loan.getId().toString(),
                request.note() == null ? "" : request.note());

        notificationService.notifyBorrowerStatusChanged(loan.getBorrower());

        return getLoan(loan.getId());
    }

    @Transactional
    public LoanResponse updateLoan(UUID loanId, LoanUpdateRequest request) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be updated");
        }

        LoanInterestSettings settings = loanInterestSettingsRepository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Loan interest settings are not configured"));

        int termDays = request.loanTermDays() != null && request.loanTermDays() > 0
                ? request.loanTermDays() : loan.getLoanTermDays();

        BigDecimal totalAmount = interestCalculationService.computeTotalAmount(request.loanAmount(), termDays, settings);

        loan.setLoanAmount(request.loanAmount());
        loan.setLoanTermDays(termDays);
        loan.setTotalAmount(totalAmount);
        loanRepository.save(loan);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPDATE_LOAN", "Loan", loan.getId().toString(),
                "amount=" + request.loanAmount() + ", term=" + termDays);

        return getLoan(loan.getId());
    }

    @Transactional
    public void cancelLoan(UUID loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be cancelled");
        }

        riskService.deleteAssessmentsByLoanId(loanId);
        loanRepository.delete(loan);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "CANCEL_LOAN", "Loan", loanId.toString(), "Pending loan cancelled");
    }


    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedule(UUID loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);
        return repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loanId).stream()
                .map(schedule -> new ScheduleResponse(
                        schedule.getInstallmentNumber(),
                        schedule.getDueDate(),
                        schedule.getAmountDue(),
                        schedule.getStatus()
                ))
                .toList();
    }

    public Loan findLoan(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Loan not found"));
    }

    @Transactional
    public void sendOverdueReminder(UUID loanId) {
        Loan loan = findLoan(loanId);
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER && currentUser.getRole() != UserRole.CASHIER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner or cashier can send payment reminders");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active loans can have payment reminders sent");
        }
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loanId);
        LocalDate today = LocalDate.now();
        boolean hasOverdue = schedules.stream().anyMatch(s ->
                s.getStatus() == ScheduleStatus.OVERDUE
                        || (s.getStatus() != ScheduleStatus.PAID && s.getDueDate() != null && s.getDueDate().isBefore(today))
        );
        if (!hasOverdue) {
            throw new ResponseStatusException(BAD_REQUEST, "This loan has no overdue or past-due installments");
        }
        Borrower borrower = borrowerService.findBorrower(loan.getBorrower().getId());
        notificationService.notifyBorrowerLoanOverdue(loan, borrower);
    }

    private void enforceBorrowerOwnershipIfNeeded(Loan loan) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.BORROWER) {
            return;
        }

        UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));

        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Borrowers can only view their own loans");
        }
    }

    private void generateSchedule(Loan loan) {
        int termDays = loan.getLoanTermDays() != null && loan.getLoanTermDays() > 0 ? loan.getLoanTermDays() : 365;
        int periodDays = loan.getInterestPeriodDays() != null && loan.getInterestPeriodDays() > 0
                ? loan.getInterestPeriodDays()
                : 30;
        int installments = Math.max(1, (termDays + periodDays - 1) / periodDays);
        BigDecimal total = loan.getTotalAmount();
        BigDecimal amountPerInstallment = total.divide(new BigDecimal(installments), 2, RoundingMode.HALF_UP);
        LocalDate issueDate = loan.getIssueDate() != null ? loan.getIssueDate() : LocalDate.now();
        for (int i = 1; i <= installments; i++) {
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);
            int daysFromIssue = Math.min(i * periodDays, termDays);
            schedule.setDueDate(issueDate.plusDays(daysFromIssue));
            schedule.setAmountDue(i == installments
                    ? total.subtract(amountPerInstallment.multiply(new BigDecimal(installments - 1)))
                    : amountPerInstallment);
            schedule.setStatus(ScheduleStatus.PENDING);
            repaymentScheduleRepository.save(schedule);
        }
    }

    private void createCashDisbursement(Loan loan) {
        User currentUser = currentUserService.requireCurrentUser();
        CashTransaction transaction = new CashTransaction();
        transaction.setLoan(loan);
        transaction.setAmount(loan.getLoanAmount());
        transaction.setType(CashTransactionType.DISBURSEMENT);
        transaction.setReferenceNumber("DISB-" + loan.getId());
        transaction.setCapturedBy(currentUser);
        transaction.setAuthorizedBy(currentUser);
        cashTransactionRepository.save(transaction);
    }

    /**
     * Build a map of loan ID to amount paid by batch querying all loans at once.
     */
    private Map<UUID, BigDecimal> buildAmountPaidMap(List<UUID> loanIds) {
        if (loanIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> results = repaymentRepository.sumAmountPaidByLoanIds(loanIds);
        return results.stream()
                .collect(Collectors.toMap(
                        m -> (UUID) m.get("loanId"),
                        m -> (BigDecimal) m.get("totalPaid")
                ));
    }

    /**
     * Convert loan to response with pre-loaded repayment data (for list endpoints).
     */
    private LoanResponse toResponse(Loan loan, BigDecimal amountPaid, List<RepaymentSchedule> schedules) {
        String borrowerUsername = null;
        String borrowerFullName = null;
        if (loan.getBorrower() != null) {
            if (loan.getBorrower().getUser() != null) {
                borrowerUsername = loan.getBorrower().getUser().getUsername();
            }
            String first = loan.getBorrower().getFirstName();
            String last = loan.getBorrower().getLastName();
            if (first != null || last != null) {
                borrowerFullName = (first != null ? first : "").trim() + " " + (last != null ? last : "").trim();
                borrowerFullName = borrowerFullName.trim();
                if (borrowerFullName.isEmpty()) borrowerFullName = null;
            }
        }

        LocalDate today = LocalDate.now();
        boolean hasOverdue = schedules.stream()
                .anyMatch(s -> s.getStatus() != ScheduleStatus.PAID && s.getDueDate() != null && s.getDueDate().isBefore(today));

        BigDecimal total = loan.getTotalAmount() != null ? loan.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal pendingAmount = total.subtract(amountPaid).max(BigDecimal.ZERO);

        return new LoanResponse(
                loan.getId(),
                loan.getBorrower().getId(),
                borrowerUsername,
                borrowerFullName,
                loan.getLoanAmount(),
                loan.getInterestRate(),
                loan.getTotalAmount(),
                pendingAmount,
                loan.getLoanTermDays(),
                loan.getIssueDate(),
                loan.getDueDate(),
                loan.getStatus(),
                loan.getRiskScore(),
                loan.getRiskBand(),
                loan.getInterestType(),
                loan.getInterestPeriodDays(),
                loan.getGracePeriodDays() != null ? loan.getGracePeriodDays() : 0,
                hasOverdue
        );
    }

    /**
     * Convert loan to response with individual queries (for single loan fetches).
     */
    private LoanResponse toResponse(Loan loan) {
        String borrowerUsername = null;
        String borrowerFullName = null;
        if (loan.getBorrower() != null) {
            if (loan.getBorrower().getUser() != null) {
                borrowerUsername = loan.getBorrower().getUser().getUsername();
            }
            String first = loan.getBorrower().getFirstName();
            String last = loan.getBorrower().getLastName();
            if (first != null || last != null) {
                borrowerFullName = (first != null ? first : "").trim() + " " + (last != null ? last : "").trim();
                borrowerFullName = borrowerFullName.trim();
                if (borrowerFullName.isEmpty()) borrowerFullName = null;
            }
        }
        boolean hasOverdue = repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loan.getId()).stream()
                .anyMatch(s -> s.getStatus() != ScheduleStatus.PAID && s.getDueDate() != null && s.getDueDate().isBefore(LocalDate.now()));
        BigDecimal total = loan.getTotalAmount() != null ? loan.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal amountPaid = repaymentRepository.sumAmountPaidByLoanId(loan.getId()) != null
                ? repaymentRepository.sumAmountPaidByLoanId(loan.getId()) : BigDecimal.ZERO;
        BigDecimal pendingAmount = total.subtract(amountPaid).max(BigDecimal.ZERO);
        return new LoanResponse(
                loan.getId(),
                loan.getBorrower().getId(),
                borrowerUsername,
                borrowerFullName,
                loan.getLoanAmount(),
                loan.getInterestRate(),
                loan.getTotalAmount(),
                pendingAmount,
                loan.getLoanTermDays(),
                loan.getIssueDate(),
                loan.getDueDate(),
                loan.getStatus(),
                loan.getRiskScore(),
                loan.getRiskBand(),
                loan.getInterestType(),
                loan.getInterestPeriodDays(),
                loan.getGracePeriodDays() != null ? loan.getGracePeriodDays() : 0,
                hasOverdue
        );
    }
}