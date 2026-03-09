package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.ActionResponse;
import com.loanshark.api.dto.ApiDtos.BorrowerSummaryResponse;
import com.loanshark.api.dto.ApiDtos.DashboardSummaryResponse;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.entity.AuditLog;
import com.loanshark.api.service.AuditLogService;
import com.loanshark.api.service.DashboardService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    public DashboardController(DashboardService dashboardService, AuditLogService auditLogService) {
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/borrower-summary")
    @PreAuthorize("hasRole('BORROWER')")
    public BorrowerSummaryResponse borrowerSummary() {
        return dashboardService.borrowerSummary();
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('OWNER')")
    public List<AuditLog> auditLogs() {
        return auditLogService.recent();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasRole('OWNER')")
    public PageResponse<ActionResponse> actions(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return dashboardService.recentActions(q, page, size);
    }
}
