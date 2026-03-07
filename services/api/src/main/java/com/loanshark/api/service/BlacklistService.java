package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos.BlacklistResponse;
import com.loanshark.api.dto.ApiDtos.BlacklistRequest;
import com.loanshark.api.dto.ApiDtos.PageResponse;
import com.loanshark.api.entity.BlacklistEntry;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.BorrowerStatus;
import com.loanshark.api.entity.User;
import com.loanshark.api.repository.BlacklistEntryRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class BlacklistService {

    private final BlacklistEntryRepository blacklistEntryRepository;
    private final BorrowerService borrowerService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public BlacklistService(
        BlacklistEntryRepository blacklistEntryRepository,
        BorrowerService borrowerService,
        CurrentUserService currentUserService,
        AuditLogService auditLogService,
        NotificationService notificationService
    ) {
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.borrowerService = borrowerService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BlacklistResponse create(BlacklistRequest request) {
        Borrower borrower = borrowerService.findBorrower(request.borrowerId());
        borrower.setStatus(BorrowerStatus.BLACKLISTED);
        User currentUser = currentUserService.requireCurrentUser();

        BlacklistEntry entry = new BlacklistEntry();
        entry.setBorrower(borrower);
        entry.setReason(request.reason());
        entry.setBlacklistedBy(currentUser);
        entry = blacklistEntryRepository.save(entry);

        auditLogService.record(currentUser.getId(), "BLACKLIST_BORROWER", "BlacklistEntry", entry.getId().toString(), request.reason());
        notificationService.notifyBorrowerStatusChanged(borrower);
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlacklistResponse> list(String query, int page, int size) {
        Page<BlacklistEntry> blacklistPage = blacklistEntryRepository.search(
            query == null ? "" : query.trim(),
            PageRequest.of(page, size)
        );
        return new PageResponse<>(
            blacklistPage.getContent().stream().map(this::toResponse).toList(),
            blacklistPage.getNumber(),
            blacklistPage.getSize(),
            blacklistPage.getTotalElements(),
            blacklistPage.getTotalPages()
        );
    }

    private BlacklistResponse toResponse(BlacklistEntry entry) {
        return new BlacklistResponse(
            entry.getId(),
            entry.getBorrower().getId(),
            entry.getReason(),
            entry.getBlacklistedAt()
        );
    }
}
