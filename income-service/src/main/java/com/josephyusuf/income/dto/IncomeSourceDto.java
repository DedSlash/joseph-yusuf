package com.josephyusuf.income.dto;

import com.josephyusuf.income.entity.IncomeSourceType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeSourceDto {

    private UUID id;
    private String name;
    private IncomeSourceType type;
    private String currency;
    private boolean active;
    private Instant createdAt;
}
