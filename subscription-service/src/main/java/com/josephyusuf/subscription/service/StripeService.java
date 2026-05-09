package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PaymentIntentResponse;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
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
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;
    private final AdminClient adminClient;

    public PaymentIntentResponse createPaymentIntent(UUID userId, PlanTier plan, String currency, String promoCode) {
        long originalAmount = resolveAmount(plan, currency);
        long finalAmount = originalAmount;
        Integer discountPercent = null;
        String appliedPromoCode = null;

        if (promoCode != null && !promoCode.isBlank()) {
            PromoCodeValidation validation = validatePromoCode(promoCode, userId);
            if (!validation.isValid()) {
                throw new PaymentException("Code promo invalide : " + validation.getReason());
            }
            discountPercent = validation.getDiscountPercent();
            appliedPromoCode = validation.getCode();
            finalAmount = applyDiscount(originalAmount, discountPercent);
            log.info("Code promo {} appliqué userId={} : {} → {} (remise {}%)",
                    appliedPromoCode, userId, originalAmount, finalAmount, discountPercent);
        }

        String idempotencyKey = String.format("pi-%s-%s-%s",
                userId, plan.name(), UUID.randomUUID().toString().substring(0, 8));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        metadata.put("plan", plan.name());
        metadata.put("idempotency_key", idempotencyKey);
        if (appliedPromoCode != null) {
            metadata.put("promoCode", appliedPromoCode);
            metadata.put("discountPercent", discountPercent.toString());
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(finalAmount)
                .setCurrency(currency.toLowerCase())
                .addPaymentMethodType("card")
                .putAllMetadata(metadata)
                .setDescription("Joseph·Yusuf — abonnement " + plan.name())
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params,
                    RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
            log.info("Stripe PaymentIntent créé id={} userId={} plan={} amount={} {}",
                    intent.getId(), userId, plan, finalAmount, currency);

            if (appliedPromoCode != null) {
                applyPromoCodeUsage(appliedPromoCode, userId, intent.getId());
            }

            return PaymentIntentResponse.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .amount(BigDecimal.valueOf(finalAmount))
                    .currency(currency.toUpperCase())
                    .status(intent.getStatus())
                    .promoCode(appliedPromoCode)
                    .discountPercent(discountPercent)
                    .originalAmount(BigDecimal.valueOf(originalAmount))
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

    private PromoCodeValidation validatePromoCode(String code, UUID userId) {
        try {
            return adminClient.validate(code, userId);
        } catch (Exception e) {
            log.error("Échec validation code promo {} userId={} : {}", code, userId, e.getMessage());
            throw new PaymentException("Validation du code promo impossible : " + e.getMessage(), e);
        }
    }

    private void applyPromoCodeUsage(String code, UUID userId, String transactionId) {
        try {
            adminClient.apply(PromoCodeApplyRequest.builder()
                    .code(code)
                    .userId(userId)
                    .transactionId(transactionId)
                    .build());
        } catch (Exception e) {
            log.error("Échec enregistrement utilisation code promo {} userId={} tx={} : {}",
                    code, userId, transactionId, e.getMessage());
        }
    }

    private long applyDiscount(long amount, int discountPercent) {
        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(amount).multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP).longValue();
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
