package com.loanshark.api.repository;

import com.loanshark.api.entity.CashTransaction;
import com.loanshark.api.entity.CashTransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {

    List<CashTransaction> findTop50ByOrderByCapturedAtDesc();

    @Query("""
        select c from CashTransaction c
        where :query = '' or
              lower(str(c.type)) like lower(concat('%', :query, '%')) or
              lower(c.referenceNumber) like lower(concat('%', :query, '%')) or
              str(c.loan.id) like concat('%', :query, '%')
        order by c.capturedAt desc
        """)
    List<CashTransaction> searchTop200(@Param("query") String query);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CashTransaction c WHERE c.type = :type")
    BigDecimal sumAmountByType(@Param("type") CashTransactionType type);
}
