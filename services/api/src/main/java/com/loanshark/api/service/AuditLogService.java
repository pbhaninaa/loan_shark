package com.loanshark.api.service;

import com.loanshark.api.entity.AuditLog;
import com.loanshark.api.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Long userId, String action, String entity, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public List<AuditLog> recent() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }
}
