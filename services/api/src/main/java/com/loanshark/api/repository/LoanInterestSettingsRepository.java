package com.loanshark.api.repository;

import com.loanshark.api.entity.LoanInterestSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanInterestSettingsRepository extends JpaRepository<LoanInterestSettings, UUID> {
}
