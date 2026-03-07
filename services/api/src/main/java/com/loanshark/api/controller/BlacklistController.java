package com.loanshark.api.controller;

import com.loanshark.api.dto.ApiDtos.BlacklistResponse;
import com.loanshark.api.dto.ApiDtos.BlacklistRequest;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.service.BlacklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blacklist")
public class BlacklistController {

    private final BlacklistService blacklistService;

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public PageResponse<BlacklistResponse> list(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return blacklistService.list(q, page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BlacklistResponse add(@Valid @RequestBody BlacklistRequest request) {
        return blacklistService.create(request);
    }
}
