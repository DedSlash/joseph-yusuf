package com.josephyusuf.admin.service;

import com.josephyusuf.admin.dto.AuditLogDto;
import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.entity.AuditLog;
import com.josephyusuf.admin.mapper.AuditLogMapper;
import com.josephyusuf.admin.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogMapper auditLogMapper;

    @InjectMocks private AuditLogService service;

    @Test
    @DisplayName("log - persiste une entrée d'audit avec tous les champs")
    void log_persists() {
        UUID adminId = UUID.randomUUID();

        service.log(adminId, "USER_BLOCK", "USER", "user-1", "raison", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(adminId);
        assertThat(saved.getAction()).isEqualTo("USER_BLOCK");
        assertThat(saved.getTargetType()).isEqualTo("USER");
        assertThat(saved.getTargetId()).isEqualTo("user-1");
        assertThat(saved.getDetails()).isEqualTo("raison");
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("list - sans filtre adminId, charge tout par date desc")
    void list_noFilter() {
        AuditLog entry = AuditLog.builder().action("X").build();
        AuditLogDto dto = AuditLogDto.builder().action("X").build();
        Page<AuditLog> page = new PageImpl<>(List.of(entry));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(auditLogMapper.toDto(entry)).thenReturn(dto);

        PageResponse<AuditLogDto> response = service.list(0, 20, null);

        assertThat(response.getContent()).containsExactly(dto);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository, never()).findAllByAdminIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("list - avec filtre adminId, utilise la requête par admin")
    void list_byAdmin() {
        UUID adminId = UUID.randomUUID();
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAllByAdminIdOrderByCreatedAtDesc(eq(adminId), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<AuditLogDto> response = service.list(0, 20, adminId);

        assertThat(response.getContent()).isEmpty();
        verify(auditLogRepository).findAllByAdminIdOrderByCreatedAtDesc(eq(adminId), any(Pageable.class));
        verify(auditLogRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }
}
