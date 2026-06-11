package com.josephyusuf.subscription.dto;

import lombok.*;

import java.util.UUID;

/**
 * Requête envoyée à alert-service via {@code POST /api/alerts/internal}.
 * Les enums {@code type} et {@code severity} sont passés en String pour
 * éviter le couplage Maven sur les enums d'alert-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalAlertRequest {

    private UUID userId;
    private String type;
    private String severity;
    private String title;
    private String message;
    private Integer month;
    private Integer year;
}
