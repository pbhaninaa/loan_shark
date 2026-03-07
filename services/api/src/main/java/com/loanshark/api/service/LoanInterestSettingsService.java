package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsResponse;
import com.loanshark.api.dto.ApiDtos.LoanInterestSettingsUpdateRequest;
import com.loanshark.api.entity.InterestType;
import com.loanshark.api.entity.LoanInterestSettings;
import com.loanshark.api.repository.LoanInterestSettingsRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanInterestSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final LoanInterestSettingsRepository repository;

    public LoanInterestSettingsService(LoanInterestSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LoanInterestSettingsResponse get() {
        LoanInterestSettings settings = repository.findById(SETTINGS_ID)
            .orElseGet(this::createDefaultSettings);
        return new LoanInterestSettingsResponse(
            settings.getDefaultInterestRate(),
            settings.getInterestType(),
            settings.getInterestPeriodDays(),
            settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0,
            settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365,
            settings.getUpdatedAt()
        );
    }

    private LoanInterestSettings createDefaultSettings() {
        LoanInterestSettings s = new LoanInterestSettings();
        s.setId(SETTINGS_ID);
        s.setDefaultInterestRate(new BigDecimal("30.00"));
        s.setInterestType(InterestType.SIMPLE);
        s.setInterestPeriodDays(30);
        s.setGracePeriodDays(0);
        s.setDefaultLoanTermDays(365);
        s.touch();
        return repository.save(s);
    }

    @Transactional
    public LoanInterestSettingsResponse update(LoanInterestSettingsUpdateRequest request) {
        LoanInterestSettings settings = repository.findById(SETTINGS_ID).orElseGet(() -> {
            LoanInterestSettings newSettings = new LoanInterestSettings();
            newSettings.setId(SETTINGS_ID);
            newSettings.setDefaultInterestRate(request.defaultInterestRate());
            newSettings.setInterestType(request.interestType());
            newSettings.setInterestPeriodDays(request.interestPeriodDays());
            newSettings.setGracePeriodDays(request.gracePeriodDays() != null ? request.gracePeriodDays() : 0);
            newSettings.setDefaultLoanTermDays(request.defaultLoanTermDays() != null ? request.defaultLoanTermDays() : 365);
            newSettings.touch();
            return repository.save(newSettings);
        });
        settings.setDefaultInterestRate(request.defaultInterestRate());
        settings.setInterestType(request.interestType());
        settings.setInterestPeriodDays(request.interestPeriodDays());
        settings.setGracePeriodDays(request.gracePeriodDays() != null ? request.gracePeriodDays() : 0);
        settings.setDefaultLoanTermDays(request.defaultLoanTermDays() != null ? request.defaultLoanTermDays() : 365);
        settings.touch();
        repository.save(settings);
        return new LoanInterestSettingsResponse(
            settings.getDefaultInterestRate(),
            settings.getInterestType(),
            settings.getInterestPeriodDays(),
            settings.getGracePeriodDays() != null ? settings.getGracePeriodDays() : 0,
            settings.getDefaultLoanTermDays() != null ? settings.getDefaultLoanTermDays() : 365,
            settings.getUpdatedAt()
        );
    }
}
