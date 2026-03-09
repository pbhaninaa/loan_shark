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
        LoanInterestSettings settings = repository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
            .orElseGet(this::createDefaultSettings);
        return new LoanInterestSettingsResponse(
            settings.getDefaultInterestRate(),
            settings.getInterestType(),
            settings.getInterestPeriodDays(),
            settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0,
            settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365,
            settings.getBorrowerLimitPercentage() != null ? settings.getBorrowerLimitPercentage() : new BigDecimal("100.00"),
            settings.getUpdatedAt()
        );
    }

    private LoanInterestSettings createDefaultSettings() {
        LoanInterestSettings s = new LoanInterestSettings();
        s.setId(UuidConstants.LOAN_INTEREST_SETTINGS_ID);
        s.setDefaultInterestRate(new BigDecimal("30.00"));
        s.setInterestType(InterestType.SIMPLE);
        s.setInterestPeriodDays(30);
        s.setGracePeriodDays(0);
        s.setDefaultLoanTermDays(365);
        s.setBorrowerLimitPercentage(new BigDecimal("100.00"));
        s.touch();
        return repository.save(s);
    }

    @Transactional
    public LoanInterestSettingsResponse update(LoanInterestSettingsUpdateRequest request) {
        LoanInterestSettings settings = repository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID).orElseGet(() -> {
            LoanInterestSettings newSettings = new LoanInterestSettings();
            newSettings.setId(UuidConstants.LOAN_INTEREST_SETTINGS_ID);
            newSettings.setDefaultInterestRate(request.defaultInterestRate());
            newSettings.setInterestType(request.interestType());
            newSettings.setInterestPeriodDays(request.interestPeriodDays());
            newSettings.setGracePeriodDays(request.gracePeriodDays() != null ? request.gracePeriodDays() : 0);
            newSettings.setDefaultLoanTermDays(request.defaultLoanTermDays() != null ? request.defaultLoanTermDays() : 365);
            newSettings.setBorrowerLimitPercentage(request.borrowerLimitPercentage() != null ? request.borrowerLimitPercentage() : new BigDecimal("100.00"));
            newSettings.touch();
            return repository.save(newSettings);
        });
        settings.setDefaultInterestRate(request.defaultInterestRate());
        settings.setInterestType(request.interestType());
        settings.setInterestPeriodDays(request.interestPeriodDays());
        settings.setGracePeriodDays(request.gracePeriodDays() != null ? request.gracePeriodDays() : 0);
        settings.setDefaultLoanTermDays(request.defaultLoanTermDays() != null ? request.defaultLoanTermDays() : 365);
        settings.setBorrowerLimitPercentage(request.borrowerLimitPercentage() != null ? request.borrowerLimitPercentage() : new BigDecimal("100.00"));
        settings.touch();
        repository.save(settings);
        return new LoanInterestSettingsResponse(
            settings.getDefaultInterestRate(),
            settings.getInterestType(),
            settings.getInterestPeriodDays(),
            settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0,
            settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365,
            settings.getBorrowerLimitPercentage() != null ? settings.getBorrowerLimitPercentage() : new BigDecimal("100.00"),
            settings.getUpdatedAt()
        );
    }

    /**
     * Compute expected amount due at end of default loan term for a given principal using current settings.
     * Principal is typically what the owner put in (total owner added); it updates as the owner continuously adds more in Business capital.
     */
    @Transactional(readOnly = true)
    public ExpectedAmountAtEndOfTermResponse getExpectedAmountAtEndOfTerm(BigDecimal principal) {
        LoanInterestSettings settings = repository.findById(UuidConstants.LOAN_INTEREST_SETTINGS_ID)
            .orElseGet(this::createDefaultSettings);
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return new ExpectedAmountAtEndOfTermResponse(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365
            );
        }
        int termDays = settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365;
        BigDecimal expectedDue = interestCalculationService.computeTotalAmount(principal, termDays, settings);
        return new ExpectedAmountAtEndOfTermResponse(principal, expectedDue, termDays);
    }
}
