package com.josephyusuf.report.mapper;

import com.josephyusuf.report.dto.ReportResponse;
import com.josephyusuf.report.entity.ReportRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    ReportResponse toResponse(ReportRecord record);
}
