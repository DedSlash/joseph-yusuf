package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PaymentProvider;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodConfigDto {
    private PaymentProvider provider;
    private boolean enabled;
    private Instant updatedAt;
}
