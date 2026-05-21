package com.josephyusuf.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelSubscriptionRequest {

    @Builder.Default
    private boolean immediately = false;
}
