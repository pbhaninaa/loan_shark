package com.loanshark.api.repository;

import com.loanshark.api.entity.BorrowerVerification;
import com.loanshark.api.entity.VerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerVerificationRepository extends JpaRepository<BorrowerVerification, Long> {

    Optional<BorrowerVerification> findTopByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    Optional<BorrowerVerification> findTopByBorrowerUserIdOrderByCreatedAtDesc(Long userId);

    List<BorrowerVerification> findTop50ByOrderByCreatedAtDesc();

    List<BorrowerVerification> findByStatusOrderByCreatedAtDesc(VerificationStatus status);

    long countByStatus(VerificationStatus status);
}
