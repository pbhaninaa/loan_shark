package com.loanshark.api.config;

import com.loanshark.api.entity.BusinessCapital;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import com.loanshark.api.entity.UuidConstants;
import com.loanshark.api.repository.BusinessCapitalRepository;
import com.loanshark.api.repository.LoanInterestSettingsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures required single-row data exists after app start (e.g. after DB was cleared or recreated).
 * So when you truncate/drop everything, the app creates tables via Flyway and this restores the
 * minimal rows needed for settings and business capital.
 */
@Component
@Order(100)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BusinessCapitalRepository businessCapitalRepository;
    private final LoanInterestSettingsRepository loanInterestSettingsRepository;

    public DataInitializer(
        BusinessCapitalRepository businessCapitalRepository,
        LoanInterestSettingsRepository loanInterestSettingsRepository
    ) {
        this.businessCapitalRepository = businessCapitalRepository;
        this.loanInterestSettingsRepository = loanInterestSettingsRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureBusinessCapital();
        ensureLoanInterestSettings();
    }

    private void ensureBusinessCapital() {
        if (businessCapitalRepository.findById(UuidConstants.BUSINESS_CAPITAL_ID).isEmpty()) {
            BusinessCapital cap = new BusinessCapital();
            cap.setId(UuidConstants.BUSINESS_CAPITAL_ID);
            cap.setBalance(BigDecimal.ZERO);
            cap.setUpdatedAt(Instant.now());
            businessCapitalRepository.save(cap);
            log.info("Created default business_capital row (balance=0)");
        }
    }

    private void ensureLoanInterestSettings() {
        if (loanInterestSettingsRepository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID).isEmpty()) {
            LoanInterestSettings s = new LoanInterestSettings();
            s.setId(UuidConstants.LOAN_INTEREST_SETTINGS_ID);
            s.setDefaultInterestRate(new BigDecimal("30.00"));
            s.setInterestType(InterestType.SIMPLE);
            s.setInterestPeriodDays(30);
            s.setGracePeriodDays(0);
            s.setDefaultLoanTermDays(365);
            s.setBorrowerLimitPercentage(new BigDecimal("100.00"));
            s.touch();
            loanInterestSettingsRepository.save(s);
            log.info("Created default loan_interest_settings row");
        }
    }
}
