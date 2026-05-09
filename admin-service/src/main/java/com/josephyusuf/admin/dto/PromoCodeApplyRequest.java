package com.josephyusuf.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeApplyRequest {

    @NotBlank
    private String code;

    @NotNull
    private UUID userId;

    private String transactionId;
}
