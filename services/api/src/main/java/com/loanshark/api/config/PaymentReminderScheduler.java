package com.loanshark.api.config;

import com.loanshark.api.entity.RepaymentSchedule;
import com.loanshark.api.entity.ScheduleStatus;
import com.loanshark.api.repository.RepaymentScheduleRepository;
import com.loanshark.api.service.NotificationService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends in-app and email reminders to borrowers 2 days before an installment due date.
 */
@Component
public class PaymentReminderScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReminderScheduler.class);

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final NotificationService notificationService;

    public PaymentReminderScheduler(
        RepaymentScheduleRepository repaymentScheduleRepository,
        NotificationService notificationService
    ) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.notificationService = notificationService;
    }

    /** Run daily at 9:00 AM; notify borrowers whose installments are due in 2 days. */
    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}")
    @Transactional
    public void sendDueInTwoDaysReminders() {
        LocalDate dueInTwoDays = LocalDate.now().plusDays(2);
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByDueDateAndStatusNot(dueInTwoDays, ScheduleStatus.PAID);
        for (RepaymentSchedule schedule : schedules) {
            try {
                notificationService.notifyPaymentDueReminder(schedule);
            } catch (Exception e) {
                LOG.warn("Failed to send payment reminder for schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
        if (!schedules.isEmpty()) {
            LOG.info("Sent {} payment-due-in-2-days reminder(s)", schedules.size());
        }
    }
}
