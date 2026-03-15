package com.loanshark.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Single-row config for loan interest: rate, simple/compound, period length.
 * Interest accrues from disbursement; each period (e.g. 30 days) adds another interest block.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loan_interest_settings")
public class LoanInterestSettings {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id = UuidConstants.LOAN_INTEREST_SETTINGS_ID;

    @Column(name = "default_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultInterestRate;

    /** Max loan amount as % of borrower's monthly salary */
    @Column(name = "borrower_limit_percentage_salary_based", precision = 5, scale = 2)
    private BigDecimal borrowerLimitPercentageSalaryBased = new BigDecimal("100.00");

    /** Max loan amount based on percentage of amount repaid on active loan */
    @Column(name = "borrower_limit_percentage_previous_loan", precision = 5, scale = 2)
    private BigDecimal borrowerLimitPercentagePreviousLoan = new BigDecimal("100.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private InterestType interestType = InterestType.SIMPLE;

    @Column(name = "interest_period_days", nullable = false)
    private Integer interestPeriodDays = 30;

    /** Days from disbursement when interest does not accumulate */
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 0;

    /** Default loan term (days) when applicant does not specify one */
    @Column(name = "default_loan_term_days", nullable = false)
    private Integer defaultLoanTermDays = 365;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void touch() {
        this.updatedAt = Instant.now();
    }
}