package com.loanshark.api.repository;

import com.loanshark.api.entity.BusinessCapital;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BusinessCapitalRepository extends JpaRepository<BusinessCapital, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM BusinessCapital bc WHERE bc.id = 1")
    Optional<BusinessCapital> findByIdForUpdate();
}
