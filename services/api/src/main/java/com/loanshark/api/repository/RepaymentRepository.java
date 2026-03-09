package com.loanshark.api.repository;

import com.loanshark.api.entity.Repayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {

    List<Repayment> findByLoanIdOrderByPaymentDateDesc(UUID loanId);

    /** Latest repayment by payment date (for generating next reference number). */
    java.util.Optional<Repayment> findFirstByOrderByPaymentDateDesc();

    @Query("SELECT COALESCE(SUM(r.amountPaid), 0) FROM Repayment r WHERE r.loan.id = :loanId")
    BigDecimal sumAmountPaidByLoanId(@Param("loanId") UUID loanId);

    long countByLoanBorrowerIdAndPaymentDateBefore(UUID borrowerId, Instant before);

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
    Page<Repayment> searchByLoanId(@Param("loanId") UUID loanId, @Param("query") String query, Pageable pageable);

    @Query("""
        select r from Repayment r
        where (
          :query = '' or
          str(r.id) like concat('%', :query, '%') or
          lower(str(r.paymentMethod)) like lower(concat('%', :query, '%')) or
          lower(r.referenceNumber) like lower(concat('%', :query, '%'))
        )
        order by r.paymentDate desc
        """)
    Page<Repayment> searchAll(@Param("query") String query, Pageable pageable);

    @Query("""
        select r from Repayment r
        where r.loan.borrower.id = :borrowerId
          and (
            :query = '' or
            str(r.id) like concat('%', :query, '%') or
            lower(str(r.paymentMethod)) like lower(concat('%', :query, '%')) or
            lower(r.referenceNumber) like lower(concat('%', :query, '%'))
          )
        order by r.paymentDate desc
        """)
    Page<Repayment> searchByBorrowerId(@Param("borrowerId") UUID borrowerId, @Param("query") String query, Pageable pageable);
}
