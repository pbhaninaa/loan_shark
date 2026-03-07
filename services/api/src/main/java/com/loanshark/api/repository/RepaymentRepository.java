package com.loanshark.api.repository;

import com.loanshark.api.entity.Repayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    List<Repayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM Repayment r WHERE r.loan.id = :loanId")
    BigDecimal sumAmountPaidByLoanId(@Param("loanId") Long loanId);

    long countByLoanBorrowerIdAndPaymentDateBefore(Long borrowerId, Instant before);

    @Query("""
        select r from Repayment r
        where r.loan.id = :loanId
          and (
            :query = '' or
            str(r.id) like concat('%', :query, '%') or
            lower(str(r.paymentMethod)) like lower(concat('%', :query, '%')) or
            lower(r.referenceNumber) like lower(concat('%', :query, '%'))
          )
        order by r.paymentDate desc
        """)
    Page<Repayment> searchByLoanId(@Param("loanId") Long loanId, @Param("query") String query, Pageable pageable);
}
