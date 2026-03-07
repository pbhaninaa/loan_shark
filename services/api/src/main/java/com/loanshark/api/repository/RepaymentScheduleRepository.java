package com.loanshark.api.repository;

import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    long countByStatus(ScheduleStatus status);

    long countByLoanBorrowerIdAndStatus(UUID borrowerId, ScheduleStatus status);
}
