package com.loanshark.api.repository;

import com.loanshark.api.entity.Borrower;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

    Optional<Borrower> findByIdNumber(String idNumber);

    Optional<Borrower> findByPhone(String phone);

    Optional<Borrower> findByUserId(UUID userId);

    long countByAddress(String address);

    List<Borrower> findAllByOrderByCreatedAtDesc();

    @Query("""
        select b from Borrower b
        where :query = '' or
              lower(b.firstName) like lower(concat('%', :query, '%')) or
              lower(b.lastName) like lower(concat('%', :query, '%')) or
              lower(b.idNumber) like lower(concat('%', :query, '%')) or
              lower(b.phone) like lower(concat('%', :query, '%')) or
              lower(coalesce(b.email, '')) like lower(concat('%', :query, '%')) or
              lower(b.address) like lower(concat('%', :query, '%'))
        """)
    Page<Borrower> search(@Param("query") String query, Pageable pageable);
}
