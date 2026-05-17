package com.josephyusuf.admin.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodConfigDto {
    private String provider;
    private boolean enabled;
    private Instant updatedAt;
}
