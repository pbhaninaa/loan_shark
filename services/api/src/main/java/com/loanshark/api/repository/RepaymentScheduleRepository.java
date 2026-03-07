package com.loanshark.api.repository;

import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    long countByStatus(ScheduleStatus status);

    long countByLoanBorrowerIdAndStatus(Long borrowerId, ScheduleStatus status);
}
