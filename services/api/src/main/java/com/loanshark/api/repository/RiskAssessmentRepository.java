package com.loanshark.api.repository;

import com.loanshark.api.entity.RiskAssessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    List<RiskAssessment> findTop10ByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);
}
