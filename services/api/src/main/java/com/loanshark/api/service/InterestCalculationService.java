package com.loanshark.api.service;

import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Interest accrues from disbursement. First period starts when the borrower gets the money
 * (even if repaid after a minute). After each period (e.g. 30 days) another interest block accumulates.
 * Grace period: for the first N days after disbursement, interest does NOT accumulate (e.g. 2 days
 * to pay without extra interest). After grace, interest applies on the remaining days.
 * - Simple: total = principal + principal * (rate/100) * numPeriods
 * - Compound: total = principal * (1 + rate/100)^numPeriods
 */
@Service
public class InterestCalculationService {

    /**
     * Number of interest periods for the given term. Minimum 1 (interest applies from day one).
     */
    public int numberOfPeriods(int loanTermDays, int periodDays) {
        if (periodDays <= 0) {
            periodDays = 30;
        }
        int periods = (loanTermDays + periodDays - 1) / periodDays;
        return Math.max(1, periods);
    }

    /**
     * Effective days that attract interest: term minus grace period. If grace >= term, no interest.
     */
    public int effectiveInterestDays(int loanTermDays, int gracePeriodDays) {
        return Math.max(0, loanTermDays - gracePeriodDays);
    }

    /**
     * Total amount due using current settings: principal + interest (simple or compound).
     * Grace period days do not attract interest (e.g. 2 days = first 2 days no interest).
     */
    public BigDecimal computeTotalAmount(
        BigDecimal principal,
        int loanTermDays,
        LoanInterestSettings settings
    ) {
        if (settings == null) {
            return principal.add(
                principal.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP)
            );
        }
        int grace = settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0;
        int effectiveDays = effectiveInterestDays(loanTermDays, grace);
        if (effectiveDays <= 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rate = settings.getDefaultInterestRate();
        int periodDays = settings.getInterestPeriodDays() != null ? settings.getInterestPeriodDays() : 30;
        int periods = numberOfPeriods(effectiveDays, periodDays);
        if (periods <= 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }

        if (settings.getInterestType() == InterestType.COMPOUND) {
            BigDecimal onePlusRate = BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
            return principal.multiply(onePlusRate.pow(periods)).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal interest = principal
            .multiply(rate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
            .multiply(new BigDecimal(periods))
            .setScale(2, RoundingMode.HALF_UP);
        return principal.add(interest).setScale(2, RoundingMode.HALF_UP);
    }
}
