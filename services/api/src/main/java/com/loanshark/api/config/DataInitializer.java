package com.loanshark.api.config;

import com.loanshark.api.entity.BusinessCapital;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import com.loanshark.api.entity.UuidConstants;
import com.loanshark.api.repository.BusinessCapitalRepository;
import com.loanshark.api.repository.LoanInterestSettingsRepository;
import com.loanshark.api.config.Constants;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

        if (businessCapitalRepository
                .findById(UuidConstants.BUSINESS_CAPITAL_ID)
                .isEmpty()) {

            BusinessCapital cap = new BusinessCapital();

            cap.setId(UuidConstants.BUSINESS_CAPITAL_ID);
            cap.setBalance(BigDecimal.ZERO);
            cap.setUpdatedAt(Instant.now());

            businessCapitalRepository.save(cap);

            log.info("Created default business_capital row (balance=0)");
        }
    }

    private void ensureLoanInterestSettings() {

        if (loanInterestSettingsRepository
                .findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .isEmpty()) {

            LoanInterestSettings s = new LoanInterestSettings();

            s.setId(UuidConstants.LOAN_INTEREST_SETTINGS_ID);

            s.setDefaultInterestRate(
                    new BigDecimal(Constants.DEFAULT_LOAN_INTEREST_RATE)
            );

            s.setInterestType(
                    InterestType.valueOf(Constants.DEFAULT_LOAN_INTEREST_TYPE)
            );

            s.setInterestPeriodDays(
                    Integer.parseInt(Constants.DEFAULT_LOAN_INTEREST_PERIOD_DAYS)
            );

            s.setGracePeriodDays(
                    Integer.parseInt(Constants.DEFAULT_LOAN_GRACE_DAYS)
            );

            s.setDefaultLoanTermDays(
                    Integer.parseInt(Constants.DEFAULT_LOAN_TERM_DAYS)
            );

            s.setBorrowerLimitPercentageSalaryBased(
                    new BigDecimal(Constants.DEFAULT_BORROWER_LIMIT_PERCENTAGE_SALARY_BASED)
            );

            s.setBorrowerLimitPercentagePreviousLoan(
                    new BigDecimal(Constants.DEFAULT_BORROWER_LIMIT_PERCENTAGE_PREVIOUS_LOAN)
            );

            s.touch();

            loanInterestSettingsRepository.save(s);

            log.info("Created default loan_interest_settings row");
        }
    }
}