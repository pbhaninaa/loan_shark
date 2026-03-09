package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.RepaymentRequest;
import com.loanshark.api.dto.ApiDtos.RepaymentResponse;
import com.loanshark.api.entity.CashTransaction;
import com.loanshark.api.entity.CashTransactionType;
import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.Repayment;
import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.service.BusinessCapitalService;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanService loanService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final BorrowerVerificationService borrowerVerificationService;
    private final NotificationService notificationService;
    private final BusinessCapitalService businessCapitalService;

    public RepaymentService(
        RepaymentRepository repaymentRepository,
        RepaymentScheduleRepository repaymentScheduleRepository,
        CashTransactionRepository cashTransactionRepository,
        BorrowerRepository borrowerRepository,
        LoanService loanService,
        CurrentUserService currentUserService,
        AuditLogService auditLogService,
        BorrowerVerificationService borrowerVerificationService,
        NotificationService notificationService,
        BusinessCapitalService businessCapitalService
    ) {
        this.repaymentRepository = repaymentRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.borrowerRepository = borrowerRepository;
        this.loanService = loanService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.borrowerVerificationService = borrowerVerificationService;
        this.notificationService = notificationService;
        this.businessCapitalService = businessCapitalService;
    }

    private static final Pattern REFERENCE_NUMBER_PATTERN = Pattern.compile("^PAY-(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Returns the next reference number for a repayment, based on the last one created (e.g. PAY-1001 → PAY-1002).
     */
    public String getNextReferenceNumber() {
        Optional<Repayment> last = repaymentRepository.findFirstByOrderByPaymentDateDesc();
        if (last.isEmpty()) {
            return "PAY-1001";
        }
        String ref = last.get().getReferenceNumber();
        if (ref == null) return "PAY-1001";
        Matcher m = REFERENCE_NUMBER_PATTERN.matcher(ref.trim());
        if (m.matches()) {
            int next = Integer.parseInt(m.group(1)) + 1;
            return "PAY-" + next;
        }
        return "PAY-1001";
    }

    /**
     * Records a payment against the selected loan. Every payment reduces the debt of the borrower
     * who owns that loan: the amount is applied to that loan's repayment schedule (installments).
     * The loan is chosen by the cashier/borrower in the UI; ensure the correct loan (payer) is selected.
     */
    @Transactional
    public RepaymentResponse record(RepaymentRequest request) {
        Loan loan = loanService.findLoan(request.loanId());
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active loans can receive repayments");
        }

        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(com.loanshark.api.entity.Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
            if (!loan.getBorrower().getId().equals(borrowerId)) {
                throw new ResponseStatusException(FORBIDDEN, "You can only record repayments for your own loans");
            }
        }
        Repayment repayment = new Repayment();
        repayment.setLoan(loan);
        repayment.setAmountPaid(request.amountPaid());
        repayment.setPaymentMethod(request.paymentMethod());
        repayment.setReferenceNumber(request.referenceNumber());
        repayment.setCapturedBy(currentUser);
        repayment = repaymentRepository.save(repayment);

        applyPaymentToSchedule(loan, request.amountPaid());
        updateLoanCompletion(loan);

        CashTransaction cashTransaction = new CashTransaction();
        cashTransaction.setLoan(loan);
        cashTransaction.setAmount(request.amountPaid());
        cashTransaction.setType(CashTransactionType.REPAYMENT);
        cashTransaction.setReferenceNumber(request.referenceNumber());
        cashTransaction.setCapturedBy(currentUser);
        cashTransaction.setAuthorizedBy(currentUser);
        cashTransactionRepository.save(cashTransaction);

        businessCapitalService.addRepayment(request.amountPaid());

        auditLogService.record(currentUser.getId(), "RECORD_REPAYMENT", "Repayment", repayment.getId().toString(), request.referenceNumber());

        String borrowerUsername = loan.getBorrower() != null && loan.getBorrower().getUser() != null
            ? loan.getBorrower().getUser().getUsername() : null;
        String borrowerFullName = loan.getBorrower() != null
            ? (loan.getBorrower().getFirstName() != null ? loan.getBorrower().getFirstName() : "").trim()
                + " " + (loan.getBorrower().getLastName() != null ? loan.getBorrower().getLastName() : "").trim()
            : null;
        if (borrowerFullName != null) borrowerFullName = borrowerFullName.trim();
        if (borrowerUsername != null) {
            notificationService.notifyUser(
                loan.getBorrower().getUser().getId(),
                "REPAYMENT",
                "Your payment of " + request.amountPaid() + " was recorded. Your debt has been reduced."
            );
        }
        return new RepaymentResponse(
            repayment.getId(),
            loan.getId(),
            borrowerUsername,
            borrowerFullName,
            repayment.getAmountPaid(),
            repayment.getPaymentDate(),
            repayment.getPaymentMethod(),
            repayment.getReferenceNumber(),
            repayment.getCapturedBy() != null ? repayment.getCapturedBy().getUsername() : null
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentResponse> listByLoan(UUID loanId, String query, int page, int size) {
        Loan loan = loanService.findLoan(loanId);
        User currentUser = currentUserService.requireCurrentUser();
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                .map(com.loanshark.api.entity.Borrower::getId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
            if (!loan.getBorrower().getId().equals(borrowerId)) {
                throw new ResponseStatusException(FORBIDDEN, "Borrowers can only view their own repayments");
            }
        }
        Page<Repayment> repaymentPage = repaymentRepository.searchByLoanId(
            loanId,
            query == null ? "" : query.trim(),
            PageRequest.of(page, size)
        );
        String borrowerUsername = loan.getBorrower() != null && loan.getBorrower().getUser() != null
            ? loan.getBorrower().getUser().getUsername() : null;
        String borrowerFullName = loan.getBorrower() != null
            ? (loan.getBorrower().getFirstName() != null ? loan.getBorrower().getFirstName() : "").trim()
                + " " + (loan.getBorrower().getLastName() != null ? loan.getBorrower().getLastName() : "").trim()
            : null;
        final String borrowerFullNameFinal = borrowerFullName != null ? borrowerFullName.trim() : null;
        return new PageResponse<>(
            repaymentPage.getContent().stream()
                .map(repayment -> new RepaymentResponse(
                    repayment.getId(),
                    repayment.getLoan().getId(),
                    borrowerUsername,
                    borrowerFullNameFinal,
                    repayment.getAmountPaid(),
                    repayment.getPaymentDate(),
                    repayment.getPaymentMethod(),
                    repayment.getReferenceNumber(),
                    repayment.getCapturedBy() != null ? repayment.getCapturedBy().getUsername() : null
                ))
                .toList(),
            repaymentPage.getNumber(),
            repaymentPage.getSize(),
            repaymentPage.getTotalElements(),
            repaymentPage.getTotalPages()
        );
    }

    /** Applies the payment to this loan's installments, reducing the paying borrower's outstanding debt. */
    private void applyPaymentToSchedule(Loan loan, BigDecimal amountPaid) {
        BigDecimal remaining = amountPaid;
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getStatus() == ScheduleStatus.PAID) {
                continue;
            }
            if (remaining.compareTo(schedule.getAmountDue()) >= 0) {
                remaining = remaining.subtract(schedule.getAmountDue());
                schedule.setAmountDue(BigDecimal.ZERO);
                schedule.setStatus(ScheduleStatus.PAID);
            } else {
                schedule.setAmountDue(schedule.getAmountDue().subtract(remaining));
                remaining = BigDecimal.ZERO;
            }
            repaymentScheduleRepository.save(schedule);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
    }

    private void updateLoanCompletion(Loan loan) {
        LoanStatus previousStatus = loan.getStatus();
        boolean outstanding = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId()).stream()
            .anyMatch(schedule -> schedule.getStatus() != ScheduleStatus.PAID);
        if (!outstanding) {
            loan.setStatus(LoanStatus.COMPLETED);
            if (previousStatus != LoanStatus.COMPLETED) {
                notificationService.notifyLoanStatusChanged(
                    loan,
                    "Your loan application #" + loan.getId() + " is now COMPLETED."
                );
            }
        }
    }
}
