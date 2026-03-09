package com.loanshark.api.repository;

import com.loanshark.api.entity.BlacklistEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntry, UUID> {

    Optional<BlacklistEntry> findTopByBorrowerIdOrderByBlacklistedAtDesc(UUID borrowerId);

    List<BlacklistEntry> findAllByOrderByBlacklistedAtDesc();

    boolean existsByBorrowerId(UUID borrowerId);

    void deleteByBorrowerId(UUID borrowerId);

    @Query("""
        select b from BlacklistEntry b
        where :query = '' or
              str(b.id) like concat('%', :query, '%') or
              lower(b.reason) like lower(concat('%', :query, '%')) or
              lower(b.borrower.firstName) like lower(concat('%', :query, '%')) or
              lower(b.borrower.lastName) like lower(concat('%', :query, '%')) or
              lower(b.borrower.phone) like lower(concat('%', :query, '%')) or
              lower(b.borrower.idNumber) like lower(concat('%', :query, '%'))
        order by b.blacklistedAt desc
        """)
    Page<BlacklistEntry> search(@Param("query") String query, Pageable pageable);
}
