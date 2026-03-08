package com.loanshark.api.repository;

import com.loanshark.api.entity.User;
import com.loanshark.api.entity.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    /** Case-insensitive lookup for login (handles "Mxo" vs "mxo"). */
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByRole(UserRole role);

    long countByRole(UserRole role);

    @Query("""
        select u from User u
        where :query = '' or
              lower(u.username) like lower(concat('%', :query, '%')) or
              lower(str(u.role)) like lower(concat('%', :query, '%')) or
              lower(str(u.status)) like lower(concat('%', :query, '%'))
        order by u.createdAt desc
        """)
    Page<User> search(@Param("query") String query, Pageable pageable);
}
