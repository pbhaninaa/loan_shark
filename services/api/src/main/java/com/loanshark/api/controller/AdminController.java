package com.loanshark.api.controller;

import com.loanshark.api.service.DatabaseResetService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final DatabaseResetService databaseResetService;

    public AdminController(DatabaseResetService databaseResetService) {
        this.databaseResetService = databaseResetService;
    }

    /**
     * Reset database: remove all history (loans, repayments, blacklist, notifications, etc.).
     * Keeps users, clients (borrowers), and their profiles. Resets business capital to zero. Owner only.
     */
    @PostMapping("/reset-history")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> resetHistory() {
        databaseResetService.resetHistory();
        return Map.of(
            "message",
            "History reset. Users, clients and their profiles kept; business capital set to zero."
        );
    }
}
