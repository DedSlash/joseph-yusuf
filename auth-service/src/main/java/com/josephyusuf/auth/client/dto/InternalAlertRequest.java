package com.josephyusuf.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalAlertRequest {

    private UUID userId;
    private String type;
    private String severity;
    private String title;
    private String message;
    private Integer month;
    private Integer year;
}
