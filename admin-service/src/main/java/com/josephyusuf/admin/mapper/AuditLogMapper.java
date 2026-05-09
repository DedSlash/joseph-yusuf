package com.josephyusuf.admin.mapper;

import com.josephyusuf.admin.dto.AuditLogDto;
import com.josephyusuf.admin.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogDto toDto(AuditLog log);
}
