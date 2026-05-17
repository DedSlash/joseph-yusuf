package com.josephyusuf.report.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationLineDto {

    @JsonAlias("category")
    private String label;
    private BigDecimal amount;
    private BigDecimal percentage;
}
