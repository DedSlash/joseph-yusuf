package com.josephyusuf.admin.service;

import com.josephyusuf.admin.dto.AuditLogDto;
import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.entity.AuditLog;
import com.josephyusuf.admin.mapper.AuditLogMapper;
import com.josephyusuf.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public void log(UUID adminId, String action, String targetType, String targetId, String details, String ip) {
        AuditLog entry = AuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ip(ip)
                .build();
        auditLogRepository.save(entry);
        log.info("Audit | admin={} action={} target={}/{}", adminId, action, targetType, targetId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> list(int page, int size, UUID adminId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> entries = adminId != null
                ? auditLogRepository.findAllByAdminIdOrderByCreatedAtDesc(adminId, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AuditLogDto> content = entries.getContent().stream()
                .map(auditLogMapper::toDto)
                .toList();

        return PageResponse.<AuditLogDto>builder()
                .content(content)
                .page(entries.getNumber())
                .size(entries.getSize())
                .totalElements(entries.getTotalElements())
                .totalPages(entries.getTotalPages())
                .build();
    }
}
