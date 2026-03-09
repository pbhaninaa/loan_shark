package com.loanshark.api.service;

import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;


@Service
public class InterestCalculationService {


    public int numberOfPeriods(int loanTermDays, int periodDays) {
        if (periodDays <= 0) {
            periodDays = 30;
        }
        int periods = (loanTermDays + periodDays - 1) / periodDays;
        return Math.max(1, periods);
    }

    public int effectiveInterestDays(int loanTermDays, int gracePeriodDays) {
        return Math.max(0, loanTermDays - gracePeriodDays);
    }


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

        BigDecimal rate = settings.getDefaultInterestRate() != null ? settings.getDefaultInterestRate() : BigDecimal.ZERO;
        int periodDays = settings.getInterestPeriodDays() != null ? settings.getInterestPeriodDays() : 30;
        if (periodDays <= 0) {
            periodDays = 30;
        }

        if (settings.getInterestType() == InterestType.COMPOUND) {
            // Fractional number of compounding periods: n = effectiveDays / periodDays
            // e.g. 364 days / 30 = 12.133... so A = P * (1+r)^12.133, not (1+r)^13
            double n = (double) effectiveDays / periodDays;
            double onePlusR = 1.0 + rate.doubleValue() / 100.0;
            double factor = Math.pow(onePlusR, n);
            return principal.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        }

        int periods = numberOfPeriods(effectiveDays, periodDays);
        if (periods <= 0) {
            return principal.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal interest = principal
            .multiply(rate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
            .multiply(new BigDecimal(periods))
            .setScale(2, RoundingMode.HALF_UP);
        return principal.add(interest).setScale(2, RoundingMode.HALF_UP);
    }
}
