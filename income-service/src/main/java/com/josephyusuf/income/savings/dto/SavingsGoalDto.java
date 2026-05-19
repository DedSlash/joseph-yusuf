package com.josephyusuf.income.savings.dto;

import com.josephyusuf.income.savings.entity.SavingsGoalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalDto {

    private UUID id;
    private UUID userId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal monthlyTarget;
    private BigDecimal monthlyTargetPercent;
    private LocalDate startDate;
    private LocalDate targetDate;
    private SavingsGoalStatus status;
    private boolean active;
    private BigDecimal progressPercent;
    private LocalDate projectedCompletionDate;
    private boolean exportAllowed;
    private Instant createdAt;
    private Instant updatedAt;
}
