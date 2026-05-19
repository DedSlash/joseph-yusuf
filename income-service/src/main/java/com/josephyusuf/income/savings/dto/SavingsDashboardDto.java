package com.josephyusuf.income.savings.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsDashboardDto {

    private BigDecimal totalSaved;
    private BigDecimal totalTarget;
    private BigDecimal globalProgressPercent;
    private long activeGoalsCount;
    private List<SavingsRecommendationDto> monthlyRecommendations;
    private NextMilestoneDto nextMilestone;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NextMilestoneDto {
        private java.util.UUID goalId;
        private String goalName;
        private BigDecimal remainingAmount;
        private BigDecimal progressPercent;
    }
}
