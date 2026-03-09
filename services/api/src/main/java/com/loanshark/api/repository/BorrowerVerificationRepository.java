package com.loanshark.api.repository;

import com.loanshark.api.entity.BorrowerVerification;
import com.loanshark.api.entity.VerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerVerificationRepository extends JpaRepository<BorrowerVerification, UUID> {

    Optional<BorrowerVerification> findTopByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

    Optional<BorrowerVerification> findTopByBorrowerUserIdOrderByCreatedAtDesc(UUID userId);

    List<BorrowerVerification> findTop50ByOrderByCreatedAtDesc();

    List<BorrowerVerification> findByStatusOrderByCreatedAtDesc(VerificationStatus status);

    long countByStatus(VerificationStatus status);

    void deleteByBorrowerId(UUID borrowerId);
}
