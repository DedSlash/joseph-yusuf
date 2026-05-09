package com.josephyusuf.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @Min(2020)
    private Integer year;
}
