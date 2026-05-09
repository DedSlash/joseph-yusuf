package com.josephyusuf.alert.dto;

import com.josephyusuf.alert.entity.AlertSeverity;
import com.josephyusuf.alert.entity.AlertType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto {

    private UUID id;
    private UUID userId;
    private AlertType type;
    private AlertSeverity severity;
    private String title;
    private String message;
    private boolean read;
    private Integer month;
    private Integer year;
    private Instant createdAt;
}
