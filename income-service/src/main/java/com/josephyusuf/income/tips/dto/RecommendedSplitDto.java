package com.josephyusuf.income.tips.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedSplitDto {

    private BigDecimal needs;
    private BigDecimal wants;
    private BigDecimal savings;
}
