package com.loanshark.api.repository;

import com.loanshark.api.entity.BusinessCapital;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessCapitalRepository extends JpaRepository<BusinessCapital, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM BusinessCapital bc WHERE bc.id = :id")
    Optional<BusinessCapital> findByIdForUpdate(@Param("id") UUID id);
}
