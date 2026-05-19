package com.josephyusuf.income.savings.dto;

import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.savings.entity.SavingsContributionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsContributionDto {

    private UUID id;
    private UUID goalId;
    private UUID userId;
    private BigDecimal amount;
    private int month;
    private int year;
    private SavingsContributionType type;
    private MonthStatus josephStatus;
    private String note;
    private Instant createdAt;
}
