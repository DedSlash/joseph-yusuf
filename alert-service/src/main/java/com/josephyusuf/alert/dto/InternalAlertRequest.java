package com.josephyusuf.alert.dto;

import com.josephyusuf.alert.entity.AlertSeverity;
import com.josephyusuf.alert.entity.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class InternalAlertRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private AlertType type;

    @NotNull
    private AlertSeverity severity;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String message;

    private Integer month;

    private Integer year;
}
