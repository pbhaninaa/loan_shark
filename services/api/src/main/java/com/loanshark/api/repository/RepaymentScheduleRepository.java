package com.loanshark.api.repository;

import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    long countByStatus(ScheduleStatus status);

    long countByLoanBorrowerIdAndStatus(UUID borrowerId, ScheduleStatus status);

    /** Schedules with this due date that are not yet paid (for reminder 2 days before). */
    List<RepaymentSchedule> findByDueDateAndStatusNot(LocalDate dueDate, ScheduleStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.loan.id FROM RepaymentSchedule s WHERE s.status = 'OVERDUE' OR (s.status <> 'PAID' AND s.dueDate < :today)")
    List<UUID> findLoanIdsWithOverdueSchedules(@org.springframework.data.repository.query.Param("today") LocalDate today);
}
