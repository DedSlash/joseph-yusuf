package com.josephyusuf.report.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportData {

    private UUID userId;
    private Integer month;
    private Integer year;
    private MonthSummaryDto summary;
    private AllocationResultDto allocation;
}
