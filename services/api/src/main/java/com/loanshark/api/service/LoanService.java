package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.LoanApplicationRequest;
import com.loanshark.api.dto.ApiDtos.LoanDecisionRequest;
import com.loanshark.api.dto.ApiDtos.LoanResponse;
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

    @Value("${app.loan.default-installments}")
    private int defaultInstallments;

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
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
        if (currentUser.getRole() == UserRole.BORROWER) {
            Long currentBorrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
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

        LoanInterestSettings settings = loanInterestSettingsRepository.findById(1L).orElse(null);
        if (settings == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Loan interest settings are not configured; contact the administrator.");
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

        auditLogService.record(currentUser.getId(), "APPLY_LOAN", "Loan", loan.getId(), String.join("; ", result.factors()));
        notificationService.notifyLoanStatusChanged(
            loan,
            "Your loan application #" + loan.getId() + " was submitted and is currently " + loan.getStatus().name() + "."
        );
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listAll(String query, int page, int size) {
        Page<Loan> loanPage = loanRepository.search(
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
    public PageResponse<LoanResponse> listMyLoans(String query, int page, int size) {
        User currentUser = currentUserService.requireCurrentUser();
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
        Long borrowerId = borrowerRepository.findByUserId(currentUser.getId())
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
    public LoanResponse getLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);
        return toResponse(loan);
    }

    @Transactional
    public LoanResponse approve(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can approve loans");
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
        auditLogService.record(currentUser.getId(), "APPROVE_LOAN", "Loan", loan.getId(), request.note() == null ? "" : request.note());
        notificationService.notifyLoanStatusChanged(
            loan,
            "Your loan application #" + loan.getId() + " was approved and is now ACTIVE."
        );
        return toResponse(loan);
    }

    @Transactional
    public LoanResponse reject(LoanDecisionRequest request) {
        Loan loan = findLoan(request.loanId());
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can reject loans");
        }
        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedBy(currentUser);
        loanRepository.save(loan);
        auditLogService.record(currentUser.getId(), "REJECT_LOAN", "Loan", loan.getId(), request.note() == null ? "" : request.note());
        notificationService.notifyLoanStatusChanged(
            loan,
            "Your loan application #" + loan.getId() + " was rejected."
        );
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedule(Long loanId) {
        Loan loan = findLoan(loanId);
        enforceBorrowerOwnershipIfNeeded(loan);
        return repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
            .map(schedule -> new ScheduleResponse(
                schedule.getInstallmentNumber(),
                schedule.getDueDate(),
                schedule.getAmountDue(),
                schedule.getStatus()
            ))
            .toList();
    }

    public Loan findLoan(Long loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Loan not found"));
    }

    private void enforceBorrowerOwnershipIfNeeded(Loan loan) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != UserRole.BORROWER) {
            return;
        }
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);

        Long borrowerId = borrowerRepository.findByUserId(currentUser.getId())
            .map(Borrower::getId)
            .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));

        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Borrowers can only view their own loans");
        }
    }

    private void generateSchedule(Loan loan) {
        int installments = loan.getLoanTermDays() >= 28 ? defaultInstallments : 1;
        BigDecimal amountPerInstallment = loan.getTotalAmount()
            .divide(new BigDecimal(installments), 2, RoundingMode.HALF_UP);
        for (int i = 1; i <= installments; i++) {
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);
            schedule.setDueDate(loan.getIssueDate().plusDays((long) i * (loan.getLoanTermDays() / installments)));
            schedule.setAmountDue(i == installments
                ? loan.getTotalAmount().subtract(amountPerInstallment.multiply(new BigDecimal(installments - 1)))
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

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
            loan.getId(),
            loan.getBorrower().getId(),
            loan.getLoanAmount(),
            loan.getInterestRate(),
            loan.getTotalAmount(),
            loan.getLoanTermDays(),
            loan.getIssueDate(),
            loan.getDueDate(),
            loan.getStatus(),
            loan.getRiskScore(),
            loan.getRiskBand(),
            loan.getInterestType(),
            loan.getInterestPeriodDays(),
            loan.getGracePeriodDays() != null ? loan.getGracePeriodDays() : 0
        );
    }
}
