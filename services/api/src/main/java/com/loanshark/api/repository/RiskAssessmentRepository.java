package com.loanshark.api.repository;

import com.loanshark.api.entity.RiskAssessment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findTop10ByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

    void deleteByLoanId(UUID loanId);

    void deleteByBorrowerId(UUID borrowerId);
}
