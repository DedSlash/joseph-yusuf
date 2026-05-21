package com.josephyusuf.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionResponse {

    private String subscriptionId;
    private String clientSecret;
    private String status;
}
