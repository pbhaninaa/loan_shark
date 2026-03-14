package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.dto.ApiDtos.RepaymentRequest;
import com.loanshark.api.dto.ApiDtos.RepaymentResponse;
import com.loanshark.api.entity.*;
import com.loanshark.api.repository.CashTransactionRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.RepaymentRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    @Transactional
    public RepaymentResponse record(RepaymentRequest request) {
        Loan loan = loanService.findLoan(request.loanId());
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active loans can receive repayments");
        }

        User currentUser = currentUserService.requireCurrentUser();

        // Borrower can only pay own loans
        if (currentUser.getRole() == UserRole.BORROWER) {
            UUID borrowerId = borrowerRepository.findByUserId(currentUser.getId())
                    .map(Borrower::getId)
                    .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Borrower profile not found"));
            if (!loan.getBorrower().getId().equals(borrowerId)) {
                throw new ResponseStatusException(FORBIDDEN, "You can only record repayments for your own loans");
            }
        }

        // ----------------------------
        // IF MOBILE_TRANSFER, VERIFY PDF PROOF
        // ----------------------------
        if ("MOBILE_TRANSFER".equalsIgnoreCase(String.valueOf(request.paymentMethod()))) {
            if (request.proof() == null || request.proof().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "Proof of payment PDF is required for MOBILE TRANSFER payments");
            }

            BigDecimal pdfAmount = extractAmountFromPdfAndValidateDate(request.proof());
            if (pdfAmount == null || pdfAmount.compareTo(request.amountPaid()) != 0) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "Payment amount does not match the amount in PDF proof. Payment rejected.");
            }
        }

        // ----------------------------
        // SAVE REPAYMENT
        // ----------------------------
        Repayment repayment = new Repayment();
        repayment.setLoan(loan);
        repayment.setAmountPaid(request.amountPaid());
        repayment.setPaymentMethod(request.paymentMethod());
        repayment.setReferenceNumber(request.referenceNumber());
        repayment.setCapturedBy(currentUser);
        repayment.setProof(request.proof());
        repayment = repaymentRepository.save(repayment);

        // Apply payment to schedule
        applyPaymentToSchedule(loan, request.amountPaid());
        updateLoanCompletion(loan);

        // Record cash transaction
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
                repayment.getCapturedBy() != null ? repayment.getCapturedBy().getUsername() : null,
                repayment.getProof()
        );
    }

    // ----------------------------
    // EXTRACT AMOUNT FROM PDF (PDFBox 3.0.6 COMPATIBLE)
    // ----------------------------
    private BigDecimal extractAmountFromPdfAndValidateDate(String base64Pdf) {
        try {
            // Decode Base64 PDF
            String base64Content = base64Pdf.contains(",") ? base64Pdf.split(",")[1] : base64Pdf;
            byte[] pdfBytes = Base64.getDecoder().decode(base64Content);

            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                // Clean whitespace
                String cleanedText = text.replaceAll("\\s+", "");

                // -----------------------
                // 1. Extract the largest numeric amount
                // -----------------------
                Pattern amountPattern = Pattern.compile(
                        "R?\\s*(\\d{1,3}(?:[ ,]\\d{3})*(?:\\.\\d{2})|\\d+\\.\\d{2})"
                );
                Matcher amountMatcher = amountPattern.matcher(cleanedText);
                BigDecimal largestAmount = null;

                while (amountMatcher.find()) {
                    String value = amountMatcher.group(1).replaceAll("[ ,]", "");
                    if (value.chars().filter(ch -> ch == '.').count() > 1) continue;

                    try {
                        BigDecimal amount = new BigDecimal(value);
                        if (largestAmount == null || amount.compareTo(largestAmount) > 0) {
                            largestAmount = amount;
                        }
                    } catch (NumberFormatException ignored) {}
                }

                if (largestAmount == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "No valid amount found in PDF proof");
                }

                // -----------------------
                // 2. Extract and validate date
                // -----------------------
                Pattern datePattern = Pattern.compile(
                        "\\b(\\d{2}/\\d{2}/\\d{4})\\b"  // matches dd/MM/yyyy
                );
                Matcher dateMatcher = datePattern.matcher(text);

                boolean validDate = false;
                java.time.LocalDate today = java.time.LocalDate.now();

                while (dateMatcher.find()) {
                    String dateStr = dateMatcher.group(1);
                    try {
                        java.time.LocalDate pdfDate = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        if (pdfDate.equals(today)) {
                            validDate = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (!validDate) {
                    throw new ResponseStatusException(BAD_REQUEST, "PDF proof date does not match today");
                }

                return largestAmount;

            }

        } catch (Exception e) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Failed to read PDF proof: " + e.getMessage()
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
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
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
        borrowerVerificationService.requireActiveBorrowerAccess(currentUser);
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

    // ----------------------------
    // APPLY PAYMENT TO SCHEDULE
    // ----------------------------
    private void applyPaymentToSchedule(Loan loan, BigDecimal amountPaid) {
        BigDecimal remaining = amountPaid;
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getStatus() == ScheduleStatus.PAID) continue;
            if (remaining.compareTo(schedule.getAmountDue()) >= 0) {
                remaining = remaining.subtract(schedule.getAmountDue());
                schedule.setAmountDue(BigDecimal.ZERO);
                schedule.setStatus(ScheduleStatus.PAID);
            } else {
                schedule.setAmountDue(schedule.getAmountDue().subtract(remaining));
                remaining = BigDecimal.ZERO;
            }
            repaymentScheduleRepository.save(schedule);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
        }
    }

    private void updateLoanCompletion(Loan loan) {
        LoanStatus previousStatus = loan.getStatus();
        boolean outstanding = repaymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId()).stream()
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