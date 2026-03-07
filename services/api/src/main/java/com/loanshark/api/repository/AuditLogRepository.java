package com.loanshark.api.repository;

import com.loanshark.api.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    @Query("""
        select a from AuditLog a
        where :query = '' or
              lower(a.action) like lower(concat('%', :query, '%')) or
              lower(a.entity) like lower(concat('%', :query, '%')) or
              lower(a.details) like lower(concat('%', :query, '%'))
        order by a.timestamp desc
        """)
    List<AuditLog> searchTop200(@Param("query") String query);
}
