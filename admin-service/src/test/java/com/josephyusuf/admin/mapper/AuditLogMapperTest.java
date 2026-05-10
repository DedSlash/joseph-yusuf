package com.josephyusuf.admin.mapper;

import com.josephyusuf.admin.dto.AuditLogDto;
import com.josephyusuf.admin.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogMapperTest {

    private final AuditLogMapper mapper = new AuditLogMapperImpl();

    @Test
    @DisplayName("toDto - mappe tous les champs")
    void toDto_full() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();
        AuditLog entity = AuditLog.builder()
                .id(id)
                .adminId(adminId)
                .action("USER_BLOCK")
                .targetType("USER")
                .targetId("user-1")
                .details("raison")
                .ip("127.0.0.1")
                .createdAt(now)
                .build();

        AuditLogDto dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getAdminId()).isEqualTo(adminId);
        assertThat(dto.getAction()).isEqualTo("USER_BLOCK");
        assertThat(dto.getTargetType()).isEqualTo("USER");
        assertThat(dto.getTargetId()).isEqualTo("user-1");
        assertThat(dto.getDetails()).isEqualTo("raison");
        assertThat(dto.getIp()).isEqualTo("127.0.0.1");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toDto - null in null out")
    void toDto_null() {
        assertThat(mapper.toDto(null)).isNull();
    }
}
