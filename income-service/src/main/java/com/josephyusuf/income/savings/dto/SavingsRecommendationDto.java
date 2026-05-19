package com.josephyusuf.income.savings.dto;

import com.josephyusuf.income.entity.MonthStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsRecommendationDto {

    private UUID goalId;
    private String goalName;
    private BigDecimal recommendedAmount;
    private MonthStatus josephStatus;
    private String message;
    private BigDecimal progressPercent;
    private LocalDate projectedCompletionDate;
    private int month;
    private int year;
}
