package com.loanshark.api.repository;

import com.loanshark.api.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
        select n from Notification n
        where n.userId = :userId
          and n.status <> :hiddenStatus
          and (
            :query = '' or
            lower(n.channel) like lower(concat('%', :query, '%')) or
            lower(n.message) like lower(concat('%', :query, '%'))
          )
        order by n.createdAt desc
        """)
    Page<Notification> searchUnreadByUserId(
        @Param("userId") UUID userId,
        @Param("hiddenStatus") String hiddenStatus,
        @Param("query") String query,
        Pageable pageable
    );

    Page<Notification> findByUserIdAndStatusNotOrderByCreatedAtDesc(UUID userId, String status, Pageable pageable);
}
