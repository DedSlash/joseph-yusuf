package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.CouponDuration;
import lombok.*;

/**
 * Résultat interne d'une création de subscription Stripe.
 * Renvoyé par {@code StripeService.createSubscription(...)} au {@code SubscriptionService}
 * pour la persistance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeSubscriptionResult {

    private String stripeSubscriptionId;
    private String stripeCustomerId;
    private String stripePriceId;
    private String clientSecret;
    private String status;
    private String appliedCouponId;
    private CouponDuration couponDuration;
}
