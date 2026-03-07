package com.loanshark.api.repository;

import com.loanshark.api.entity.LoanInterestSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanInterestSettingsRepository extends JpaRepository<LoanInterestSettings, Long> {
}
