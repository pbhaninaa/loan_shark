package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.RepaymentRequest;
import com.loanshark.api.dto.ApiDtos.RepaymentResponse;
import com.loanshark.api.entity.*;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;

import com.loanshark.api.util.ValidationUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanService loanService;
    private final BorrowerService borrowerService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final BorrowerVerificationService borrowerVerificationService;
    private final NotificationService notificationService;
    private final BusinessCapitalService businessCapitalService;

    private static final Pattern REFERENCE_NUMBER_PATTERN = Pattern.compile("^PAY-(\\d+)$", Pattern.CASE_INSENSITIVE);

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
            BusinessCapitalService businessCapitalService,
            BorrowerService borrowerService
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
        this.borrowerService = borrowerService;
    }

    // ----------------------------
    // REFERENCE NUMBER GENERATOR
    // ----------------------------
    public String getNextReferenceNumber() {
        Optional<Repayment> last = repaymentRepository.findFirstByOrderByPaymentDateDesc();
        if (last.isEmpty()) return "PAY-1001";

        String ref = last.get().getReferenceNumber();
        if (ref == null) return "PAY-1001";

        Matcher m = REFERENCE_NUMBER_PATTERN.matcher(ref.trim());
        if (m.matches()) {
            int next = Integer.parseInt(m.group(1)) + 1;
            return "PAY-" + next;
        }
        return "PAY-1001";
    }

    // ----------------------------
    // RECORD REPAYMENT
    // ----------------------------
    /**
     * Records a payment against the selected loan. The borrower is not bound to installment amounts:
     * they can pay in full, pay what they can afford, or pay any amount at any time. The payment
     * is applied to the schedule in order (oldest unpaid first), reducing debt until the loan is paid off.
     */
    @Transactional
    public RepaymentResponse record(RepaymentRequest request) {

        // ✅ 1. VALIDATE OUTSIDE HEAVY DB WORK
        if ("MOBILE_TRANSFER".equalsIgnoreCase(String.valueOf(request.paymentMethod()))) {
            if (request.proof() == null || request.proof().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Proof of payment PDF is required for MOBILE TRANSFER payments");
            }

            BigDecimal pdfAmount = ValidationUtil.extractAmountFromPdfAndValidateDate(request.proof());

            if (pdfAmount == null || pdfAmount.compareTo(request.amountPaid()) != 0) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Payment amount does not match the amount in PDF proof.");
            }
        }

        // ✅ 2. LOCK BUSINESS CAPITAL FIRST (CRITICAL FIX)
        businessCapitalService.addRepayment(request.amountPaid());

        // ✅ 3. THEN LOAD LOAN
        Loan loan = loanService.findLoan(request.loanId());

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active loans can receive repayments");
        }

        // ✅ 4. SECURITY CHECK
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                    .map(Borrower::getId)
                    .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));

            if (!loan.getBorrower().getId().equals(borrowerId)) {
                throw new ResponseStatusException(FORBIDDEN, "You can only record repayments for your own loans");
            }
        }

        // ✅ 5. SAVE REPAYMENT
        Repayment repayment = new Repayment();
        repayment.setLoan(loan);
        repayment.setAmountPaid(request.amountPaid());
        repayment.setPaymentMethod(request.paymentMethod());
        repayment.setReferenceNumber(request.referenceNumber());
        repayment.setCapturedBy(currentUser);

        repayment = repaymentRepository.save(repayment);

        // ✅ 6. APPLY PAYMENT (OPTIMIZED)
        applyPaymentToScheduleOptimized(loan, request.amountPaid());

        // ✅ 7. UPDATE LOAN STATUS
        updateLoanCompletion(loan);

        // ✅ 8. SAVE CASH TRANSACTION
        CashTransaction cashTransaction = new CashTransaction();
        cashTransaction.setLoan(loan);
        cashTransaction.setAmount(request.amountPaid());
        cashTransaction.setType(CashTransactionType.REPAYMENT);
        cashTransaction.setReferenceNumber(request.referenceNumber());
        cashTransaction.setCapturedBy(currentUser);
        cashTransaction.setAuthorizedBy(currentUser);

        cashTransactionRepository.save(cashTransaction);

        // ✅ 9. AUDIT (OK inside TX)
        auditLogService.record(
                currentUser.getId(),
                "RECORD_REPAYMENT",
                "Repayment",
                repayment.getId().toString(),
                request.referenceNumber()
        );

        // ⚠️ 10. NOTIFICATION (SHOULD BE ASYNC)
        notifyBorrowerAsync(loan, request.amountPaid());
        String borrowerUsername = loan.getBorrower() != null && loan.getBorrower().getUser() != null
                ? loan.getBorrower().getUser().getUsername()
                : null;

        String borrowerFullName = loan.getBorrower() != null
                ? (loan.getBorrower().getFirstName() != null ? loan.getBorrower().getFirstName() : "").trim()
                + " " + (loan.getBorrower().getLastName() != null ? loan.getBorrower().getLastName() : "").trim()
                : null;

        if (borrowerFullName != null) borrowerFullName = borrowerFullName.trim();
        return new RepaymentResponse(
                repayment.getId(),
                loan.getId(),
                borrowerUsername,
                borrowerFullName,
                repayment.getAmountPaid(),
                repayment.getPaymentDate(),
                repayment.getPaymentMethod(),
                repayment.getReferenceNumber(),
                repayment.getCapturedBy() != null ? repayment.getCapturedBy().getUsername() : null,
                repayment.getProof()
        );
    }
    private void applyPaymentToScheduleOptimized(Loan loan, BigDecimal paymentAmount) {

        List<RepaymentSchedule> schedules =
                repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loan.getId());

        BigDecimal remainingPayment = paymentAmount;

        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getStatus() == ScheduleStatus.PAID) continue;

            BigDecimal due = schedule.getAmountDue();

            if (remainingPayment.compareTo(due) >= 0) {
                schedule.setAmountDue(BigDecimal.ZERO);
                schedule.setStatus(ScheduleStatus.PAID);
                remainingPayment = remainingPayment.subtract(due);
            } else {
                schedule.setAmountDue(due.subtract(remainingPayment));
                schedule.setStatus(ScheduleStatus.PENDING);
                remainingPayment = BigDecimal.ZERO;
            }

            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;
        }

        // ✅ ONE DB CALL INSTEAD OF MANY
        repaymentScheduleRepository.saveAll(schedules);
    }
    @Async
    public void notifyBorrowerAsync(Loan loan, BigDecimal amount) {
        if (loan.getBorrower() != null && loan.getBorrower().getUser() != null) {
            notificationService.notifyUser(
                    loan.getBorrower().getUser().getId(),
                    "REPAYMENT",
                    "Your payment of " + amount + " was recorded. Your debt has been reduced."
            );
        }
    }
    // ----------------------------
    // LISTING METHODS
    // ----------------------------
    @Transactional(readOnly = true)
    public PageResponse<RepaymentResponse> listByLoan(UUID loanId, String query, int page, int size) {
        Loan loan = loanService.findLoan(loanId);
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                    .map(Borrower::getId)
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
                        .map(repayment -> toRepaymentResponse(repayment, borrowerUsername, borrowerFullNameFinal))
                        .toList(),
                repaymentPage.getNumber(),
                repaymentPage.getSize(),
                repaymentPage.getTotalElements(),
                repaymentPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RepaymentResponse> listAll(String query, int page, int size) {
        User currentUser = currentUserService.requireCurrentUser();
        Page<Repayment> repaymentPage;
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                    .map(Borrower::getId)
                    .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
            repaymentPage = repaymentRepository.searchByBorrowerId(
                    borrowerId,
                    query == null ? "" : query.trim(),
                    PageRequest.of(page, size)
            );
        } else {
            repaymentPage = repaymentRepository.searchAll(
                    query == null ? "" : query.trim(),
                    PageRequest.of(page, size)
            );
        }
        return new PageResponse<>(
                repaymentPage.getContent().stream()
                        .map(this::toRepaymentResponse)
                        .toList(),
                repaymentPage.getNumber(),
                repaymentPage.getSize(),
                repaymentPage.getTotalElements(),
                repaymentPage.getTotalPages()
        );
    }

    private RepaymentResponse toRepaymentResponse(Repayment repayment) {
        Loan loan = repayment.getLoan();
        String borrowerUsername = loan.getBorrower() != null && loan.getBorrower().getUser() != null
                ? loan.getBorrower().getUser().getUsername() : null;
        String borrowerFullName = loan.getBorrower() != null
                ? (loan.getBorrower().getFirstName() != null ? loan.getBorrower().getFirstName() : "").trim()
                + " " + (loan.getBorrower().getLastName() != null ? loan.getBorrower().getLastName() : "").trim()
                : null;
        if (borrowerFullName != null) borrowerFullName = borrowerFullName.trim();
        return toRepaymentResponse(repayment, borrowerUsername, borrowerFullName);
    }

    private RepaymentResponse toRepaymentResponse(Repayment repayment, String borrowerUsername, String borrowerFullName) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getLoan().getId(),
                borrowerUsername,
                borrowerFullName,
                repayment.getAmountPaid(),
                repayment.getPaymentDate(),
                repayment.getPaymentMethod(),
                repayment.getReferenceNumber(),
                repayment.getCapturedBy() != null ? repayment.getCapturedBy().getUsername() : null,
                repayment.getProof()
        );
    }


    private void applyPaymentToSchedule(Loan loan, BigDecimal paymentAmount) {
        List<RepaymentSchedule> schedules = repaymentScheduleRepository
                .findByLoanIdOrderPendingFirst(loan.getId());

        BigDecimal remainingPayment = paymentAmount;

        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getStatus() == ScheduleStatus.PAID) continue;

            BigDecimal due = schedule.getAmountDue();
            if (remainingPayment.compareTo(due) >= 0) {
                // Full installment paid
                schedule.setAmountDue(BigDecimal.ZERO);
                schedule.setStatus(ScheduleStatus.PAID);
                remainingPayment = remainingPayment.subtract(due);
            } else {
                // Partial payment
                schedule.setAmountDue(due.subtract(remainingPayment));
                schedule.setStatus(ScheduleStatus.PENDING);
                remainingPayment = BigDecimal.ZERO;
            }

            repaymentScheduleRepository.save(schedule);

            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;
        }
    }
    private void updateLoanCompletion(Loan loan) {
        LoanStatus previousStatus = loan.getStatus();
        boolean outstanding = repaymentScheduleRepository.findByLoanIdOrderPendingFirst(loan.getId()).stream()
                .anyMatch(schedule -> schedule.getStatus() != ScheduleStatus.PAID);
        if (!outstanding) {
            loan.setStatus(LoanStatus.COMPLETED);
            if (previousStatus != LoanStatus.COMPLETED) {
                Borrower borrower = borrowerService.findBorrower(loan.getBorrower().getId());
                notificationService.notifyBorrowerStatusChanged(borrower);
            }
        }
    }
}