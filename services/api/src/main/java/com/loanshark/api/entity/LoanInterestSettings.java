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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private InterestType interestType = InterestType.SIMPLE;

    @Column(name = "interest_period_days", nullable = false)
    private Integer interestPeriodDays = 30;

    /** Days from disbursement when interest does not accumulate (e.g. 2 = 2 days grace to pay without extra interest). */
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 0;

    /** Default loan term (days) when applicant does not specify one; nominal term for due date; actual payoff by repayments. */
    @Column(name = "default_loan_term_days", nullable = false)
    private Integer defaultLoanTermDays = 365;

    /** Max loan amount as % of borrower's monthly salary (e.g. 25 = client can borrow up to 25% of monthly income). */
    @Column(name = "borrower_limit_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal borrowerLimitPercentage = new BigDecimal("100.00");

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
