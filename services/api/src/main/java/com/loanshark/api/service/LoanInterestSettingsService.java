package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.ExpectedAmountAtEndOfTermResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsUpdateRequest;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import com.loanshark.api.entity.UuidConstants;
import com.loanshark.api.repository.LoanInterestSettingsRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanInterestSettingsService {

    private static final BigDecimal DEFAULT_LIMIT = BigDecimal.valueOf(100);
    private static final int DEFAULT_TERM = 365;
    private static final int DEFAULT_GRACE = 0;

    private final LoanInterestSettingsRepository repository;
    private final InterestCalculationService interestCalculationService;

    public LoanInterestSettingsService(
            LoanInterestSettingsRepository repository,
            InterestCalculationService interestCalculationService
    ) {
        this.repository = repository;
        this.interestCalculationService = interestCalculationService;
    }

    @Transactional
    public LoanInterestSettingsResponse get() {

        LoanInterestSettings settings = repository
                .findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);

        BigDecimal salaryLimit =
                settings.getBorrowerLimitPercentageSalaryBased() != null
                        ? settings.getBorrowerLimitPercentageSalaryBased()
                        : DEFAULT_LIMIT;

        BigDecimal previousLoanLimit =
                settings.getBorrowerLimitPercentagePreviousLoan() != null
                        ? settings.getBorrowerLimitPercentagePreviousLoan()
                        : DEFAULT_LIMIT;

        int graceDays =
                settings.getGracePeriodDays() != null
                        ? settings.getGracePeriodDays()
                        : DEFAULT_GRACE;

        int termDays =
                settings.getDefaultLoanTermDays() != null
                        ? settings.getDefaultLoanTermDays()
                        : DEFAULT_TERM;

        return new LoanInterestSettingsResponse(
                settings.getDefaultInterestRate(),
                settings.getInterestType(),
                settings.getInterestPeriodDays(),
                graceDays,
                termDays,
                salaryLimit,
                previousLoanLimit,
                settings.getUpdatedAt(),
                settings.getBorrowerLimitPercentageSalaryBased(),
                settings.getBorrowerLimitPercentagePreviousLoan()
        );
    }

    private LoanInterestSettings createDefaultSettings() {

        LoanInterestSettings s = new LoanInterestSettings();

        s.setId(UuidConstants.LOAN_INTEREST_SETTINGS_ID);
        s.setDefaultInterestRate(new BigDecimal("30.00"));
        s.setInterestType(InterestType.SIMPLE);
        s.setInterestPeriodDays(30);
        s.setGracePeriodDays(DEFAULT_GRACE);
        s.setDefaultLoanTermDays(DEFAULT_TERM);
        s.setBorrowerLimitPercentageSalaryBased(DEFAULT_LIMIT);
        s.setBorrowerLimitPercentagePreviousLoan(DEFAULT_LIMIT);

        s.touch();

        return repository.save(s);
    }

    @Transactional
    public LoanInterestSettingsResponse update(
            LoanInterestSettingsUpdateRequest request
    ) {

        LoanInterestSettings settings = repository
                .findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);

        settings.setDefaultInterestRate(request.defaultInterestRate());
        settings.setInterestType(request.interestType());
        settings.setInterestPeriodDays(request.interestPeriodDays());

        settings.setGracePeriodDays(
                request.gracePeriodDays() != null ? request.gracePeriodDays() : DEFAULT_GRACE
        );

        settings.setDefaultLoanTermDays(
                request.defaultLoanTermDays() != null ? request.defaultLoanTermDays() : DEFAULT_TERM
        );

        settings.setBorrowerLimitPercentageSalaryBased(
                request.borrowerLimitPercentageSalaryBased() != null
                        ? request.borrowerLimitPercentageSalaryBased()
                        : DEFAULT_LIMIT
        );

        settings.setBorrowerLimitPercentagePreviousLoan(
                request.borrowerLimitPercentagePreviousLoan() != null
                        ? request.borrowerLimitPercentagePreviousLoan()
                        : DEFAULT_LIMIT
        );

        settings.touch();

        repository.save(settings);

        // Calculate values for response
        BigDecimal salaryLimit =
                settings.getBorrowerLimitPercentageSalaryBased() != null
                        ? settings.getBorrowerLimitPercentageSalaryBased()
                        : DEFAULT_LIMIT;

        BigDecimal previousLoanLimit =
                settings.getBorrowerLimitPercentagePreviousLoan() != null
                        ? settings.getBorrowerLimitPercentagePreviousLoan()
                        : DEFAULT_LIMIT;

        int graceDays =
                settings.getGracePeriodDays() != null
                        ? settings.getGracePeriodDays()
                        : DEFAULT_GRACE;

        int termDays =
                settings.getDefaultLoanTermDays() != null
                        ? settings.getDefaultLoanTermDays()
                        : DEFAULT_TERM;

        return new LoanInterestSettingsResponse(
                settings.getDefaultInterestRate(),
                settings.getInterestType(),
                settings.getInterestPeriodDays(),
                graceDays,
                termDays,
                salaryLimit,
                previousLoanLimit,
                settings.getUpdatedAt(),
                settings.getBorrowerLimitPercentageSalaryBased(),
                settings.getBorrowerLimitPercentagePreviousLoan()
        );
    }

    @Transactional(readOnly = true)
    public ExpectedAmountAtEndOfTermResponse getExpectedAmountAtEndOfTerm(
            BigDecimal principal
    ) {

        LoanInterestSettings settings = repository
                .findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);

        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return new ExpectedAmountAtEndOfTermResponse(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    settings.getDefaultLoanTermDays()
            );
        }

        int termDays = settings.getDefaultLoanTermDays();

        BigDecimal expectedDue =
                interestCalculationService.computeTotalAmount(
                        principal,
                        termDays,
                        settings
                );

        return new ExpectedAmountAtEndOfTermResponse(
                principal,
                expectedDue,
                termDays
        );
    }
}