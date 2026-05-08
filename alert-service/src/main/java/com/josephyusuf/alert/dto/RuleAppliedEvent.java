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
public class RuleAppliedEvent {

    private UUID userId;
    private String rule;
    private BigDecimal totalIncome;
    private String monthStatus;
    private Integer month;
    private Integer year;
    private Instant occurredAt;
}
