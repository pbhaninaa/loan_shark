package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.RiskCheckResponse;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.LoanStatus;
import com.loanshark.api.entity.RiskAssessment;
import com.loanshark.api.entity.RiskBand;
import com.loanshark.api.repository.BlacklistEntryRepository;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.LoanRepository;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import com.loanshark.api.repository.RiskAssessmentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RiskService {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final BorrowerRepository borrowerRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    @Value("${app.loan.max-loan-multiplier}")
    private BigDecimal maxLoanMultiplier;

    public RiskService(
        LoanRepository loanRepository,
        RepaymentScheduleRepository repaymentScheduleRepository,
        BorrowerRepository borrowerRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        RiskAssessmentRepository riskAssessmentRepository
    ) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.borrowerRepository = borrowerRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    public RiskCheckResponse assess(Borrower borrower, BigDecimal requestedAmount) {
        int score = 0;
        List<String> factors = new ArrayList<>();

        long activeLoans = loanRepository.countByBorrowerIdAndStatusIn(
            borrower.getId(),
            List.of(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.ACTIVE)
        );
        if (activeLoans > 0) {
            score += 20;
            factors.add("Borrower already has unpaid or pending loans");
        }
        if (activeLoans > 3) {
            score += 30;
            factors.add("Borrower has more than three active loans");
        }

        boolean defaultHistory = loanRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId()).stream()
            .anyMatch(loan -> loan.getStatus() == LoanStatus.DEFAULTED);
        if (defaultHistory) {
            score += 40;
            factors.add("Borrower has default history");
        }

        if (borrower.getMonthlyIncome().multiply(maxLoanMultiplier).compareTo(requestedAmount) < 0) {
            score += 20;
            factors.add("Requested loan is too high compared to monthly income");
        }

        if (repaymentScheduleRepository.countByLoanBorrowerIdAndStatus(
            borrower.getId(),
            com.loanshark.api.entity.ScheduleStatus.OVERDUE
        ) > 0) {
            score += 15;
            factors.add("Borrower has overdue repayment history");
        }

        if (loanRepository.findTop5ByBorrowerIdOrderByCreatedAtDesc(borrower.getId()).size() >= 3) {
            score += 10;
            factors.add("Borrower is applying frequently");
        }

        if (borrowerRepository.countByAddress(borrower.getAddress()) > 1) {
            score += 10;
            factors.add("Address matches multiple borrower profiles");
        }

        if (blacklistEntryRepository.existsByBorrowerId(borrower.getId())) {
            score += 100;
            factors.add("Borrower is blacklisted");
        }

        if (loanRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId()).isEmpty()) {
            score += 10;
            factors.add("New borrower with no repayment history");
        }

        score = Math.min(score, 100);
        return new RiskCheckResponse(score, bandFor(score), factors);
    }

    public RiskAssessment persistAssessment(Borrower borrower, Loan loan, RiskCheckResponse result) {
        borrower.setRiskScore(result.score());
        borrowerRepository.save(borrower);

        RiskAssessment assessment = new RiskAssessment();
        assessment.setBorrower(borrower);
        assessment.setLoan(loan);
        assessment.setScore(result.score());
        assessment.setBand(result.band());
        assessment.setSummary(String.join("; ", result.factors()));
        return riskAssessmentRepository.save(assessment);
    }

    private RiskBand bandFor(int score) {
        if (score <= 30) {
            return RiskBand.SAFE;
        }
        if (score <= 60) {
            return RiskBand.MEDIUM_RISK;
        }
        return RiskBand.HIGH_RISK;
    }
}
