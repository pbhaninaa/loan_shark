package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.*;
import com.loanshark.api.entity.*;
import com.loanshark.api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

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
    private final LoanInterestSettingsService loanInterestSettingsService;

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
            CashTransactionRepository cashTransactionRepository,
            RepaymentScheduleRepository repaymentScheduleRepository,
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
            RepaymentRepository repaymentRepository, LoanInterestSettingsService loanInterestSettingsService
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
        this.loanInterestSettingsService = loanInterestSettingsService;
    }

    // -------------------- APPLY LOAN --------------------
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
            BigDecimal limitPercent = loanInterestSettingsService.get().borrowerLimitPercentage(); // e.g., 10 = 10%
            if (paid == null) paid = BigDecimal.ZERO;

// Convert percentage to decimal
            BigDecimal requiredMin = totalOwed.multiply(limitPercent).divide(BigDecimal.valueOf(100));

            if (paid.compareTo(requiredMin) < 0) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "You must pay at least " + limitPercent + "% of your current loan " +
                                "(Loan #" + active.getId() + ": " + paid + " of " + totalOwed + " paid) before applying for a new loan."
                );
            }
        }

        User currentUser = currentUserService.requireCurrentUser();
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
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
                    "Insufficient funds to disburse this loan. Available: " + available + ". Required: " + request.loanAmount()
            );
        }

        LoanInterestSettings settings = loanInterestSettingsRepository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Loan interest settings are not configured"));

        BigDecimal limitPct = settings.getBorrowerLimitPercentage() != null ? settings.getBorrowerLimitPercentage() : BigDecimal.valueOf(100);
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
                ? settings.getDefaultLoanTermDays() : 365;
        int termDays = request.loanTermDays() != null && request.loanTermDays() > 0
                ? request.loanTermDays() : defaultTerm;

        BigDecimal totalAmount = interestCalculationService.computeTotalAmount(
                request.loanAmount(), termDays, settings
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

    // -------------------- LIST LOANS --------------------
    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listAll(String query, List<LoanStatus> statuses, int page, int size) {
        Page<Loan> loanPage;
        if (statuses != null && !statuses.isEmpty()) {
            loanPage = loanRepository.searchByStatusIn(
                    query == null ? "" : query.trim(),
                    statuses,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            loanPage = loanRepository.search(
                    query == null ? "" : query.trim(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }
        return new PageResponse<>(
                loanPage.getContent().stream().map(this::toResponse).toList(),
                loanPage.getNumber(),
                loanPage.getSize(),
                loanPage.getTotalElements(),
                loanPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listMyLoans(String query, int page, int size) {
        User currentUser = currentUserService.requireCurrentUser();
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
        UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Borrower profile not found"));

        Page<Loan> loanPage = loanRepository.searchMyLoans(
                borrowerId,
                query == null ? "" : query.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PageResponse<>(
                loanPage.getContent().stream().map(this::toResponse).toList(),
                loanPage.getNumber(),
                loanPage.getSize(),
                loanPage.getTotalElements(),
                loanPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(UUID loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);
        return toResponse(loan);
    }

    @Transactional
    public LoanResponse approve(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER && !(currentUser.getRole() == UserRole.CASHIER && loan.getLoanAmount().compareTo(cashierApprovalLimit) < 0)) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can approve loans over limit; cashiers may approve smaller loans");
        }
        if (loan.getStatus() != LoanStatus.PENDING) throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be approved");

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(loan.getLoanTermDays()));
        loan.setApprovedBy(currentUser);
        loanRepository.save(loan);

        generateSchedule(loan);
        businessCapitalService.deductForDisbursement(loan.getLoanAmount());
        createCashDisbursement(loan);
        auditLogService.record(currentUser.getId(), "APPROVE_LOAN", "Loan", loan.getId().toString(), request.note() == null ? "" : request.note());
        notificationService.notifyLoanApproved(loan);

        return toResponse(loan);
    }

    @Transactional
    public LoanResponse updateLoan(UUID loanId, LoanUpdateRequest request) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be updated");

        LoanInterestSettings settings = loanInterestSettingsRepository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Loan interest settings are not configured"));

        int termDays = request.loanTermDays() != null && request.loanTermDays() > 0 ? request.loanTermDays() : loan.getLoanTermDays();
        BigDecimal totalAmount = interestCalculationService.computeTotalAmount(request.loanAmount(), termDays, settings);

        loan.setLoanAmount(request.loanAmount());
        loan.setLoanTermDays(termDays);
        loan.setTotalAmount(totalAmount);
        loanRepository.save(loan);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "UPDATE_LOAN", "Loan", loan.getId().toString(), "amount=" + request.loanAmount() + ", term=" + termDays);

        return toResponse(loan);
    }

    @Transactional
    public void cancelLoan(UUID loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.PENDING) throw new ResponseStatusException(BAD_REQUEST, "Only pending loans can be cancelled");

        riskService.deleteAssessmentsByLoanId(loanId);
        loanRepository.delete(loan);

        User currentUser = currentUserService.requireCurrentUser();
        auditLogService.record(currentUser.getId(), "CANCEL_LOAN", "Loan", loanId.toString(), "Pending loan cancelled");
    }

    @Transactional
    public LoanResponse reject(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER && !(currentUser.getRole() == UserRole.CASHIER && loan.getLoanAmount().compareTo(cashierApprovalLimit) < 0)) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can reject loans over limit; cashiers may reject smaller loans");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(currentUser);
        loanRepository.save(loan);

        auditLogService.record(currentUser.getId(), "REJECT_LOAN", "Loan", loan.getId().toString(), request.note() == null ? "" : request.note());
        Borrower borrower = borrowerService.findBorrower(loan.getBorrower().getId());
        notificationService.notifyBorrowerStatusChanged(borrower);

        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedule(UUID loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);
        return repaymentScheduleRepository.findByLoanIdOrderByStatusPendingFirst(loanId).stream()
                .map(s -> new ScheduleResponse(s.getInstallmentNumber(), s.getDueDate(), s.getAmountDue(), s.getStatus()))
                .toList();
    }

    @Transactional
    public void sendOverdueReminder(UUID loanId) {
        Loan loan = findLoan(loanId);
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER && currentUser.getRole() != UserRole.CASHIER) throw new ResponseStatusException(FORBIDDEN, "Only owner or cashier can send reminders");
        if (loan.getStatus() != LoanStatus.ACTIVE) throw new ResponseStatusException(BAD_REQUEST, "Only active loans can have payment reminders sent");

        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        LocalDate today = LocalDate.now();
        boolean hasOverdue = schedules.stream().anyMatch(s ->
                s.getStatus() == ScheduleStatus.OVERDUE || (s.getStatus() != ScheduleStatus.PAID && s.getDueDate().isBefore(today))
        );
        if (!hasOverdue) throw new ResponseStatusException(BAD_REQUEST, "This loan has no overdue or past-due installments");

        Borrower borrower = borrowerService.findBorrower(loan.getBorrower().getId());
        notificationService.notifyBorrowerLoanOverdue(loan, borrower);
    }

    @Transactional
    public void recordPayment(UUID loanId, BigDecimal amount) {
        Loan loan = findLoan(loanId);
        CashTransaction payment = new CashTransaction();
        payment.setLoan(loan);
        payment.setAmount(amount);
        payment.setType(CashTransactionType.REPAYMENT);
        payment.setReferenceNumber("PAY-" + UUID.randomUUID());
        payment.setCapturedBy(currentUserService.requireCurrentUser());
        cashTransactionRepository.save(payment);

        recalculateScheduleAfterPayment(loan);
    }

    @Transactional
    public void recalculateScheduleAfterPayment(Loan loan) {
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        BigDecimal totalPaid = repaymentRepository.sumAmountPaidByLoanId(loan.getId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal remaining = loan.getTotalAmount().subtract(totalPaid).max(BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            schedules.forEach(s -> {
                if (s.getStatus() != ScheduleStatus.PAID) {
                    s.setAmountDue(BigDecimal.ZERO);
                    s.setStatus(ScheduleStatus.PAID);
                }
            });
            repaymentScheduleRepository.saveAll(schedules);
            return;
        }

        int remainingInstallments = (int) schedules.stream().filter(s -> s.getStatus() != ScheduleStatus.PAID).count();
        if (remainingInstallments == 0) return;

        BigDecimal baseAmount = remaining.divide(BigDecimal.valueOf(remainingInstallments), 2, RoundingMode.DOWN);
        BigDecimal scheduledTotal = BigDecimal.ZERO;
        LocalDate loanDueDate = loan.getDueDate() != null ? loan.getDueDate() : loan.getIssueDate().plusDays(loan.getLoanTermDays());

        int i = 1;
        for (RepaymentSchedule s : schedules) {
            if (s.getStatus() == ScheduleStatus.PAID) continue;

            BigDecimal amount = (i == remainingInstallments) ? remaining.subtract(scheduledTotal) : baseAmount;
            if (i != remainingInstallments) scheduledTotal = scheduledTotal.add(amount);

            s.setAmountDue(amount);
            if (s.getDueDate().isAfter(loanDueDate)) s.setDueDate(loanDueDate);
            s.setStatus(ScheduleStatus.PENDING);
            i++;
        }
        repaymentScheduleRepository.saveAll(schedules);
    }

    private void generateSchedule(Loan loan) {
        repaymentScheduleRepository.deleteByLoanId(loan.getId());
        int termDays = loan.getLoanTermDays() != null && loan.getLoanTermDays() > 0 ? loan.getLoanTermDays() : 365;
        int periodDays = loan.getInterestPeriodDays() != null && loan.getInterestPeriodDays() > 0 ? loan.getInterestPeriodDays() : 30;
        BigDecimal total = loan.getTotalAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("Loan total must be > 0");

        int installments = Math.max(1, (termDays + periodDays - 1) / periodDays);
        BigDecimal amountPerInstallment = total.divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);

        LocalDate issueDate = loan.getIssueDate() != null ? loan.getIssueDate() : LocalDate.now();
        BigDecimal scheduledTotal = BigDecimal.ZERO;

        for (int i = 1; i <= installments; i++) {
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);

            int daysFromIssue = Math.min(i * periodDays, termDays);
            LocalDate dueDate = issueDate.plusDays(daysFromIssue);

            if (loan.getDueDate() != null && dueDate.isAfter(loan.getDueDate())) {
                dueDate = loan.getDueDate();
            }
            schedule.setDueDate(dueDate);

            BigDecimal amount;
            if (i == installments) amount = total.subtract(scheduledTotal);
            else {
                amount = amountPerInstallment;
                scheduledTotal = scheduledTotal.add(amount);
            }

            schedule.setAmountDue(amount);
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

    Loan findLoan(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Loan not found"));
    }

    private void enforceBorrowerOwnershipIfNeeded(Loan loan) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId()).map(Borrower::getId).orElseThrow();
            if (!loan.getBorrower().getId().equals(borrowerId)) {
                throw new ResponseStatusException(FORBIDDEN, "Borrowers can only access their own loans");
            }
        }
    }

    private LoanResponse toResponse(Loan loan) {
        String borrowerUsername = null;
        String borrowerFullName = null;
        if (loan.getBorrower() != null && loan.getBorrower().getUser() != null) {
            borrowerUsername = loan.getBorrower().getUser().getUsername();
        }
        String first = loan.getBorrower() != null ? loan.getBorrower().getFirstName() : null;
        String last = loan.getBorrower() != null ? loan.getBorrower().getLastName() : null;
        if (first != null || last != null) {
            borrowerFullName = (first != null ? first : "") + " " + (last != null ? last : "");
        }

        boolean hasOverdue = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId())
                .stream().anyMatch(s -> s.getStatus() != ScheduleStatus.PAID && s.getDueDate().isBefore(LocalDate.now()));

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