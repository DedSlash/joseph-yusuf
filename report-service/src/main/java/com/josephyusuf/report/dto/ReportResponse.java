package com.josephyusuf.report.dto;

import com.josephyusuf.report.entity.ReportType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private UUID id;
    private ReportType type;
    private Integer month;
    private Integer year;
    private String fileName;
    private Integer sizeBytes;
    private Instant createdAt;
}
