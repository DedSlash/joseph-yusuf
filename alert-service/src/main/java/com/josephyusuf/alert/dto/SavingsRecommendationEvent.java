package com.josephyusuf.alert.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsRecommendationEvent {

    private UUID userId;
    private UUID goalId;
    private String goalName;
    private BigDecimal recommendedAmount;
    private String josephStatus;
    private String message;
    private int month;
    private int year;
    private Instant occurredAt;
}
