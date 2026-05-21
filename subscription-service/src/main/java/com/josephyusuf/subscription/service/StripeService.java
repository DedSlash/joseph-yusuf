package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.dto.StripeSubscriptionResult;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.CouponDuration;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Discount;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;
    private final AdminClient adminClient;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Crée une subscription Stripe récurrente avec coupon optionnel.
     * <p>
     * Flow :
     * <ol>
     *     <li>Validation du code promo interne (admin-service) si fourni</li>
     *     <li>Récupération/création du Customer Stripe (avec metadata userId)</li>
     *     <li>Attache du payment method au customer et défini par défaut</li>
     *     <li>Création de la Subscription avec {@code payment_behavior=default_incomplete}</li>
     *     <li>Retour du {@code clientSecret} pour confirmation côté frontend</li>
     * </ol>
     * Le coupon (s'il est valide en interne) est appliqué via {@code setCoupon(code)} sur Stripe.
     * Stripe gère ensuite ONCE / FOREVER / REPEATING sur toutes les invoices futures.
     */
    public StripeSubscriptionResult createSubscription(UUID userId,
                                                       String email,
                                                       PlanTier planTier,
                                                       String currency,
                                                       String couponCode,
                                                       String paymentMethodId) {
        if (planTier == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }

        String appliedCouponId = validateAndResolveCoupon(couponCode, userId);

        try {
            Customer customer = getOrCreateStripeCustomer(userId, email);
            attachAndDefaultPaymentMethod(customer, paymentMethodId);

            String priceId = resolvePriceId(planTier, currency);
            com.stripe.model.Subscription subscription = createStripeSubscription(
                    userId, customer.getId(), priceId, appliedCouponId, planTier);

            CouponDuration couponDuration = extractCouponDuration(subscription);
            String clientSecret = extractClientSecret(subscription);

            log.info("Stripe Subscription créée sub={} customer={} userId={} plan={} {} coupon={}",
                    subscription.getId(), customer.getId(), userId, planTier, currency, appliedCouponId);

            return StripeSubscriptionResult.builder()
                    .stripeSubscriptionId(subscription.getId())
                    .stripeCustomerId(customer.getId())
                    .stripePriceId(priceId)
                    .clientSecret(clientSecret)
                    .status(subscription.getStatus())
                    .appliedCouponId(appliedCouponId)
                    .couponDuration(couponDuration)
                    .build();
        } catch (StripeException e) {
            log.error("Échec création Stripe Subscription userId={} : {}", userId, e.getMessage());
            throw new PaymentException("L'abonnement n'a pas pu être créé. Veuillez réessayer.", e);
        }
    }

    /**
     * Annule une subscription Stripe.
     *
     * @param immediately {@code true} pour annulation immédiate, {@code false} pour
     *                    annulation en fin de période (recommandé — le client garde
     *                    l'accès jusqu'à la fin de la période payée).
     */
    public com.stripe.model.Subscription cancelSubscription(String stripeSubscriptionId, boolean immediately) {
        try {
            com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            if (immediately) {
                return subscription.cancel(SubscriptionCancelParams.builder().build());
            }
            return subscription.update(SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build());
        } catch (StripeException e) {
            log.error("Échec annulation Stripe Subscription={} : {}", stripeSubscriptionId, e.getMessage());
            throw new PaymentException("L'annulation n'a pas pu être effectuée. Veuillez réessayer.", e);
        }
    }

    public com.stripe.model.Subscription retrieveSubscription(String stripeSubscriptionId) {
        try {
            return com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        } catch (StripeException e) {
            log.error("Impossible de récupérer la Subscription={} : {}", stripeSubscriptionId, e.getMessage());
            throw new PaymentException("Impossible de vérifier l'état de l'abonnement.", e);
        }
    }

    String resolvePriceId(PlanTier planTier, String currency) {
        if (planTier == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE n'a pas de Price ID");
        }
        boolean eur = "EUR".equalsIgnoreCase(currency);
        return switch (planTier) {
            case PREMIUM -> eur ? stripeConfig.getPremiumMonthlyEurPriceId() : stripeConfig.getPremiumMonthlyXofPriceId();
            case PREMIUM_PLUS -> eur ? stripeConfig.getPremiumPlusMonthlyEurPriceId() : stripeConfig.getPremiumPlusMonthlyXofPriceId();
            default -> throw new InvalidPlanException("Plan non supporté pour Stripe : " + planTier);
        };
    }

    Customer getOrCreateStripeCustomer(UUID userId, String email) throws StripeException {
        Optional<Subscription> existing = subscriptionRepository.findByUserId(userId);
        if (existing.isPresent() && existing.get().getStripeCustomerId() != null) {
            return Customer.retrieve(existing.get().getStripeCustomerId());
        }
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        return Customer.create(CustomerCreateParams.builder()
                .setEmail(email)
                .putAllMetadata(metadata)
                .build());
    }

    private void attachAndDefaultPaymentMethod(Customer customer, String paymentMethodId) throws StripeException {
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        pm.attach(PaymentMethodAttachParams.builder()
                .setCustomer(customer.getId())
                .build());
        customer.update(CustomerUpdateParams.builder()
                .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build());
    }

    private com.stripe.model.Subscription createStripeSubscription(UUID userId,
                                                                   String customerId,
                                                                   String priceId,
                                                                   String couponId,
                                                                   PlanTier planTier) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", userId.toString());
        metadata.put("plan", planTier.name());

        SubscriptionCreateParams.Builder builder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .addExpand("latest_invoice.payment_intent")
                .putAllMetadata(metadata);

        if (couponId != null && !couponId.isBlank()) {
            builder.setCoupon(couponId);
        }

        return com.stripe.model.Subscription.create(builder.build());
    }

    private String validateAndResolveCoupon(String couponCode, UUID userId) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        PromoCodeValidation validation;
        try {
            validation = adminClient.validate(couponCode, userId);
        } catch (Exception e) {
            log.error("Échec validation code promo {} userId={} : {}", couponCode, userId, e.getMessage());
            throw new PaymentException("Impossible de valider le code promo. Veuillez réessayer.", e);
        }
        if (!validation.isValid()) {
            throw new PaymentException("Code promo invalide : " + validation.getReason());
        }
        // Le code interne validé doit correspondre à un coupon Stripe portant le même ID.
        return validation.getCode();
    }

    private CouponDuration extractCouponDuration(com.stripe.model.Subscription subscription) {
        Discount discount = subscription.getDiscount();
        if (discount == null || discount.getCoupon() == null) {
            return null;
        }
        return toCouponDuration(discount.getCoupon());
    }

    static CouponDuration toCouponDuration(Coupon coupon) {
        if (coupon == null || coupon.getDuration() == null) {
            return null;
        }
        return switch (coupon.getDuration().toLowerCase()) {
            case "once" -> CouponDuration.ONCE;
            case "forever" -> CouponDuration.FOREVER;
            case "repeating" -> CouponDuration.MONTHS;
            default -> null;
        };
    }

    private String extractClientSecret(com.stripe.model.Subscription subscription) {
        Invoice latestInvoice = subscription.getLatestInvoiceObject();
        if (latestInvoice == null) {
            return null;
        }
        PaymentIntent intent = latestInvoice.getPaymentIntentObject();
        return intent == null ? null : intent.getClientSecret();
    }
}
