package com.loanshark.api.service;

import com.loanshark.api.entity.AuditLog;
import com.loanshark.api.repository.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** @param entityId stored as string (e.g. entity.getId().toString() for UUID entities). */
    public void record(UUID userId, String action, String entity, String entityId, String details) {
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
