package com.loanshark.api.repository;

import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {
    @org.springframework.data.jpa.repository.Query("""
    SELECT s FROM RepaymentSchedule s 
    WHERE s.loan.id = :loanId
    ORDER BY 
        CASE s.status 
            WHEN 'PENDING' THEN 1
            WHEN 'OVERDUE' THEN 2
            WHEN 'PAID' THEN 3
            ELSE 4
        END,
        s.installmentNumber ASC
""")
    List<RepaymentSchedule> findByLoanIdOrderByStatusPendingFirst(@org.springframework.data.repository.query.Param("loanId") UUID loanId);
    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    long countByStatus(ScheduleStatus status);

    long countByLoanBorrowerIdAndStatus(UUID borrowerId, ScheduleStatus status);

    /** Schedules with this due date that are not yet paid (for reminder 2 days before). */
    List<RepaymentSchedule> findByDueDateAndStatusNot(LocalDate dueDate, ScheduleStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.loan.id FROM RepaymentSchedule s WHERE s.status = 'OVERDUE' OR (s.status <> 'PAID' AND s.dueDate < :today)")
    List<UUID> findLoanIdsWithOverdueSchedules(@org.springframework.data.repository.query.Param("today") LocalDate today);

    void deleteByLoanId(UUID id);

    @Query("""
        SELECT s FROM RepaymentSchedule s
        WHERE s.loan.id IN :loanIds
        ORDER BY s.loan.id, s.installmentNumber ASC
        """)
    List<RepaymentSchedule> findByLoanIdsOrderByInstallmentNumber(@Param("loanIds") List<UUID> loanIds);
}
