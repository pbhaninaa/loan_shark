package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.BusinessCapitalResponse;
import com.loanshark.api.entity.BusinessCapital;
import com.loanshark.api.entity.CashTransactionType;
import com.loanshark.api.entity.UuidConstants;
import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.BusinessCapitalRepository;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.RepaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class BusinessCapitalService {

    private final BusinessCapitalRepository businessCapitalRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public BusinessCapitalService(
        BusinessCapitalRepository businessCapitalRepository,
        CashTransactionRepository cashTransactionRepository,
        LoanRepository loanRepository,
        RepaymentRepository repaymentRepository,
        CurrentUserService currentUserService,
        AuditLogService auditLogService
    ) {
        this.businessCapitalRepository = businessCapitalRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance() {
        return businessCapitalRepository.findById(UuidConstants.BUSINESS_CAPITAL_ID)
            .map(BusinessCapital::getBalance)
            .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BusinessCapitalResponse getSummary() {
        BigDecimal balance = getBalance();
        BigDecimal totalMoneyOut = cashTransactionRepository.sumAmountByType(CashTransactionType.DISBURSEMENT);
        BigDecimal totalMoneyIn = cashTransactionRepository.sumAmountByType(CashTransactionType.REPAYMENT);
        if (totalMoneyOut == null) totalMoneyOut = BigDecimal.ZERO;
        if (totalMoneyIn == null) totalMoneyIn = BigDecimal.ZERO;
        // Pending payments = what clients still need to pay (total owed minus already paid per active loan)
        BigDecimal expectedAmount = BigDecimal.ZERO;
        for (Loan loan : loanRepository.findAllByStatusOrderByCreatedAtAsc(LoanStatus.ACTIVE)) {
            BigDecimal paid = repaymentRepository.sumAmountPaidByLoanId(loan.getId());
            if (paid == null) paid = BigDecimal.ZERO;
            BigDecimal pending = loan.getTotalAmount().subtract(paid);
            if (pending.compareTo(BigDecimal.ZERO) > 0) {
                expectedAmount = expectedAmount.add(pending);
            }
        }
        return new BusinessCapitalResponse(balance, totalMoneyOut, totalMoneyIn, expectedAmount);
    }

    /**
     * Owner adds funds (initial capital or top-up). Interest from repayments is added automatically when repayments are recorded.
     */
    @Transactional
    public void addFunds(BigDecimal amount) {
        if (currentUserService.requireCurrentUser().getRole() != UserRole.OWNER) {
            throw new ResponseStatusException(FORBIDDEN, "Only owner can add funds to business capital");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Amount must be greater than zero");
        }
        BusinessCapital cap = businessCapitalRepository.findByIdForUpdate(UuidConstants.BUSINESS_CAPITAL_ID)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Business capital record not found"));
        cap.setBalance(cap.getBalance().add(amount));
        cap.setUpdatedAt(Instant.now());
        businessCapitalRepository.save(cap);
        auditLogService.record(
            currentUserService.requireCurrentUser().getId(),
            "ADD_FUNDS",
            "BusinessCapital",
            cap.getId().toString(),
            "Added " + amount + " to lending pool. New balance: " + cap.getBalance()
        );
    }

    /**
     * Deduct when a loan is disbursed. Call only inside a transaction that approves the loan.
     */
    @Transactional
    public void deductForDisbursement(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BusinessCapital cap = businessCapitalRepository.findByIdForUpdate(UuidConstants.BUSINESS_CAPITAL_ID)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Business capital record not found"));
        if (cap.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Insufficient funds. Available: " + cap.getBalance() + ". Required for this loan: " + amount + ". Ask the admin to add funds."
            );
        }
        cap.setBalance(cap.getBalance().subtract(amount));
        cap.setUpdatedAt(Instant.now());
        businessCapitalRepository.save(cap);
    }

    /**
     * Add repayment amount (principal + interest) back to the lending pool. Call when a repayment is recorded.
     */
    @Transactional
    public void addRepayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BusinessCapital cap = businessCapitalRepository.findByIdForUpdate(UuidConstants.BUSINESS_CAPITAL_ID)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Business capital record not found"));
        cap.setBalance(cap.getBalance().add(amount));
        cap.setUpdatedAt(Instant.now());
        businessCapitalRepository.save(cap);
    }
}
