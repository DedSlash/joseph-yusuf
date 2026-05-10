package com.josephyusuf.admin.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {

    private UUID id;
    private UUID adminId;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private String ip;
    private Instant createdAt;
}
