package com.josephyusuf.alert.mapper;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.entity.Alert;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    AlertDto toDto(Alert alert);
}
