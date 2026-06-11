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
    private String displayName;
    private int displayOrder;
    private String paytechMethodCode;
    private String routing;
    private Instant updatedAt;
}
