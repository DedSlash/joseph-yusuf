package com.josephyusuf.income.tips.dto;

import com.josephyusuf.income.entity.MonthStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyTipsDto {

    private MonthStatus josephStatus;
    private BigDecimal totalAmount;
    private String currency;
    private String country;
    private BigDecimal recommendedSavings;
    private RecommendedSplitDto recommendedSplit;
    private List<MoneyTipDto> tips;
}
