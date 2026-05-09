package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PaymentIntentResponse;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;

    public PaymentIntentResponse createPaymentIntent(UUID userId, PlanTier plan, String currency) {
        long amountInSmallestUnit = resolveAmount(plan, currency);
        String idempotencyKey = String.format("pi-%s-%s-%s",
                userId, plan.name(), UUID.randomUUID().toString().substring(0, 8));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        metadata.put("plan", plan.name());
        metadata.put("idempotency_key", idempotencyKey);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInSmallestUnit)
                .setCurrency(currency.toLowerCase())
                .addPaymentMethodType("card")
                .putAllMetadata(metadata)
                .setDescription("Joseph·Yusuf — abonnement " + plan.name())
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params,
                    RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
            log.info("Stripe PaymentIntent créé id={} userId={} plan={} amount={} {}",
                    intent.getId(), userId, plan, amountInSmallestUnit, currency);
            return PaymentIntentResponse.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .amount(BigDecimal.valueOf(amountInSmallestUnit))
                    .currency(currency.toUpperCase())
                    .status(intent.getStatus())
                    .build();
        } catch (StripeException e) {
            log.error("Échec Stripe PaymentIntent userId={} : {}", userId, e.getMessage());
            throw new PaymentException("Échec création PaymentIntent Stripe : " + e.getMessage(), e);
        }
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            throw new PaymentException("Impossible de récupérer le PaymentIntent : " + paymentIntentId, e);
        }
    }

    private long resolveAmount(PlanTier plan, String currency) {
        if (plan == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }
        boolean eur = "EUR".equalsIgnoreCase(currency);
        return switch (plan) {
            case PREMIUM -> eur ? stripeConfig.getPremiumPriceEur() : stripeConfig.getPremiumPriceXof();
            case PREMIUM_PLUS -> eur ? stripeConfig.getPremiumPlusPriceEur() : stripeConfig.getPremiumPlusPriceXof();
            default -> throw new InvalidPlanException("Plan non supporté : " + plan);
        };
    }
}
