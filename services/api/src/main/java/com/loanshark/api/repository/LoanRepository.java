package com.loanshark.api.repository;

import com.loanshark.api.entity.Loan;
import com.loanshark.api.entity.LoanStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

    List<Loan> findByBorrowerIdAndStatus(UUID borrowerId, LoanStatus status);

    long countByBorrowerIdAndStatusIn(UUID borrowerId, List<LoanStatus> statuses);

    List<Loan> findAllByStatusOrderByCreatedAtAsc(LoanStatus status);

    List<Loan> findTop5ByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

    @Query("SELECT COALESCE(SUM(l.totalAmount), 0) FROM Loan l WHERE l.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") LoanStatus status);

    @Query("""
        select l from Loan l
        where :query = '' or
              str(l.id) like concat('%', :query, '%') or
              lower(str(l.status)) like lower(concat('%', :query, '%')) or
              lower(str(l.riskBand)) like lower(concat('%', :query, '%')) or
              lower(l.borrower.firstName) like lower(concat('%', :query, '%')) or
              lower(l.borrower.lastName) like lower(concat('%', :query, '%')) or
              lower(l.borrower.phone) like lower(concat('%', :query, '%')) or
              lower(l.borrower.idNumber) like lower(concat('%', :query, '%'))
        """)
    Page<Loan> search(@Param("query") String query, Pageable pageable);

    @Query("""
        select l from Loan l
        where l.status in :statuses
          and (:query = '' or
              str(l.id) like concat('%', :query, '%') or
              lower(str(l.status)) like lower(concat('%', :query, '%')) or
              lower(str(l.riskBand)) like lower(concat('%', :query, '%')) or
              lower(l.borrower.firstName) like lower(concat('%', :query, '%')) or
              lower(l.borrower.lastName) like lower(concat('%', :query, '%')) or
              lower(l.borrower.phone) like lower(concat('%', :query, '%')) or
              lower(l.borrower.idNumber) like lower(concat('%', :query, '%')))
        """)
    Page<Loan> searchByStatusIn(@Param("query") String query, @Param("statuses") List<LoanStatus> statuses, Pageable pageable);

    @Query("""
    select l from Loan l
    where l.borrower.id = :borrowerId
      and (:query = '' 
           or str(l.id) like concat('%', :query, '%')
           or lower(str(l.status)) like lower(concat('%', :query, '%'))
           or lower(str(l.riskBand)) like lower(concat('%', :query, '%')))
    order by
      case l.status
          when com.loanshark.api.entity.LoanStatus.PENDING then 0
          else 1
      end,
      l.status asc
""")
    Page<Loan> searchMyLoans(
            @Param("borrowerId") UUID borrowerId,
            @Param("query") String query,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"borrower", "borrower.user"})
    @Query("""
    SELECT l FROM Loan l
    WHERE l.borrower.id = :borrowerId
      AND (:query IS NULL OR :query = ''
           OR lower(l.borrower.firstName) LIKE lower(concat('%', :query, '%'))
           OR lower(l.borrower.lastName) LIKE lower(concat('%', :query, '%')))
""")
    Page<Loan> searchWithBorrowerAndUser(
            @Param("query") String query,
            @Param("borrowerId") UUID borrowerId,
            Pageable pageable
    );
}
