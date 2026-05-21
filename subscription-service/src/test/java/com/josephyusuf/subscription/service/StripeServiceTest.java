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
import com.stripe.exception.ApiException;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Discount;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock private StripeConfig stripeConfig;
    @Mock private AdminClient adminClient;
    @Mock private SubscriptionRepository subscriptionRepository;

    private StripeService stripeService;
    private MockedStatic<com.stripe.model.Subscription> subMock;
    private MockedStatic<Customer> customerMock;
    private MockedStatic<PaymentMethod> pmMock;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(stripeConfig, adminClient, subscriptionRepository);
        subMock = mockStatic(com.stripe.model.Subscription.class);
        customerMock = mockStatic(Customer.class);
        pmMock = mockStatic(PaymentMethod.class);
    }

    @AfterEach
    void tearDown() {
        subMock.close();
        customerMock.close();
        pmMock.close();
    }

    @Test
    @DisplayName("resolvePriceId → PREMIUM EUR")
    void resolvePriceId_premiumEur() {
        when(stripeConfig.getPremiumMonthlyEurPriceId()).thenReturn("price_premium_eur");
        assertThat(stripeService.resolvePriceId(PlanTier.PREMIUM, "EUR")).isEqualTo("price_premium_eur");
    }

    @Test
    @DisplayName("resolvePriceId → PREMIUM XOF")
    void resolvePriceId_premiumXof() {
        when(stripeConfig.getPremiumMonthlyXofPriceId()).thenReturn("price_premium_xof");
        assertThat(stripeService.resolvePriceId(PlanTier.PREMIUM, "XOF")).isEqualTo("price_premium_xof");
    }

    @Test
    @DisplayName("resolvePriceId → PREMIUM_PLUS EUR")
    void resolvePriceId_premiumPlusEur() {
        when(stripeConfig.getPremiumPlusMonthlyEurPriceId()).thenReturn("price_pp_eur");
        assertThat(stripeService.resolvePriceId(PlanTier.PREMIUM_PLUS, "EUR")).isEqualTo("price_pp_eur");
    }

    @Test
    @DisplayName("resolvePriceId → FREE → InvalidPlanException")
    void resolvePriceId_free() {
        assertThatThrownBy(() -> stripeService.resolvePriceId(PlanTier.FREE, "EUR"))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("getOrCreateStripeCustomer → customer existant récupéré, pas recréé")
    void getOrCreateCustomer_existing() throws Exception {
        Subscription existing = Subscription.builder().userId(userId).stripeCustomerId("cus_existing").build();
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn("cus_existing");
        customerMock.when(() -> Customer.retrieve("cus_existing")).thenReturn(customer);

        Customer result = stripeService.getOrCreateStripeCustomer(userId, "u@josephyusuf.com");

        assertThat(result.getId()).isEqualTo("cus_existing");
        customerMock.verify(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class)), never());
    }

    @Test
    @DisplayName("getOrCreateStripeCustomer → nouveau si aucun stripeCustomerId")
    void getOrCreateCustomer_new() throws Exception {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        Customer created = mock(Customer.class);
        when(created.getId()).thenReturn("cus_new");
        customerMock.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenReturn(created);

        Customer result = stripeService.getOrCreateStripeCustomer(userId, "u@josephyusuf.com");

        assertThat(result.getId()).isEqualTo("cus_new");
    }

    @Test
    @DisplayName("createSubscription sans coupon → pas de setCoupon Stripe")
    void createSubscription_withoutCoupon() {
        when(stripeConfig.getPremiumMonthlyEurPriceId()).thenReturn("price_p_eur");
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        mockCustomer("cus_new");
        PaymentMethod pm = mock(PaymentMethod.class);
        pmMock.when(() -> PaymentMethod.retrieve("pm_x")).thenReturn(pm);

        com.stripe.model.Subscription stripeSub = mockStripeSub("sub_1", "incomplete", null);
        ArgumentCaptor<SubscriptionCreateParams> captor = ArgumentCaptor.forClass(SubscriptionCreateParams.class);
        subMock.when(() -> com.stripe.model.Subscription.create(captor.capture())).thenReturn(stripeSub);

        StripeSubscriptionResult result = stripeService.createSubscription(
                userId, "u@josephyusuf.com", PlanTier.PREMIUM, "EUR", null, "pm_x");

        assertThat(result.getStripeSubscriptionId()).isEqualTo("sub_1");
        assertThat(result.getStripeCustomerId()).isEqualTo("cus_new");
        assertThat(result.getAppliedCouponId()).isNull();
        verify(adminClient, never()).validate(any(), any());
    }

    @Test
    @DisplayName("createSubscription avec coupon EARLY50 valide → setCoupon appliqué")
    void createSubscription_withValidCoupon() {
        when(stripeConfig.getPremiumMonthlyEurPriceId()).thenReturn("price_p_eur");
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(adminClient.validate("EARLY50", userId)).thenReturn(
                PromoCodeValidation.builder().code("EARLY50").discountPercent(50).valid(true).build());

        mockCustomer("cus_new");
        PaymentMethod pm = mock(PaymentMethod.class);
        pmMock.when(() -> PaymentMethod.retrieve("pm_x")).thenReturn(pm);

        Coupon coupon = mock(Coupon.class);
        when(coupon.getDuration()).thenReturn("forever");
        Discount discount = mock(Discount.class);
        when(discount.getCoupon()).thenReturn(coupon);
        com.stripe.model.Subscription stripeSub = mockStripeSub("sub_1", "incomplete", discount);
        subMock.when(() -> com.stripe.model.Subscription.create(any(SubscriptionCreateParams.class))).thenReturn(stripeSub);

        StripeSubscriptionResult result = stripeService.createSubscription(
                userId, "u@josephyusuf.com", PlanTier.PREMIUM, "EUR", "EARLY50", "pm_x");

        assertThat(result.getAppliedCouponId()).isEqualTo("EARLY50");
        assertThat(result.getCouponDuration()).isEqualTo(CouponDuration.FOREVER);
    }

    @Test
    @DisplayName("createSubscription avec coupon invalide → PaymentException avant appel Stripe")
    void createSubscription_invalidCoupon() {
        when(adminClient.validate("EXPIRED", userId)).thenReturn(
                PromoCodeValidation.builder().valid(false).reason("Code promo expiré").build());

        assertThatThrownBy(() -> stripeService.createSubscription(
                userId, "u@josephyusuf.com", PlanTier.PREMIUM, "EUR", "EXPIRED", "pm_x"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("expiré");

        subMock.verify(() -> com.stripe.model.Subscription.create(any(SubscriptionCreateParams.class)), never());
    }

    @Test
    @DisplayName("createSubscription FREE → InvalidPlanException")
    void createSubscription_free() {
        assertThatThrownBy(() -> stripeService.createSubscription(
                userId, "u@josephyusuf.com", PlanTier.FREE, "EUR", null, "pm_x"))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("createSubscription : StripeException → PaymentException")
    void createSubscription_stripeException() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        customerMock.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class)))
                .thenThrow(new ApiException("Stripe down", "req_1", "api_error", 500, null));

        assertThatThrownBy(() -> stripeService.createSubscription(
                userId, "u@josephyusuf.com", PlanTier.PREMIUM, "EUR", null, "pm_x"))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    @DisplayName("cancelSubscription(immediately=false) → setCancelAtPeriodEnd(true)")
    void cancelSubscription_atPeriodEnd() throws Exception {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        subMock.when(() -> com.stripe.model.Subscription.retrieve("sub_1")).thenReturn(stripeSub);
        com.stripe.model.Subscription updated = mock(com.stripe.model.Subscription.class);
        ArgumentCaptor<SubscriptionUpdateParams> captor = ArgumentCaptor.forClass(SubscriptionUpdateParams.class);
        when(stripeSub.update(captor.capture())).thenReturn(updated);

        com.stripe.model.Subscription result = stripeService.cancelSubscription("sub_1", false);

        assertThat(result).isSameAs(updated);
        verify(stripeSub, never()).cancel(any(SubscriptionCancelParams.class));
    }

    @Test
    @DisplayName("cancelSubscription(immediately=true) → cancel() immédiat")
    void cancelSubscription_immediate() throws Exception {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        subMock.when(() -> com.stripe.model.Subscription.retrieve("sub_1")).thenReturn(stripeSub);
        com.stripe.model.Subscription cancelled = mock(com.stripe.model.Subscription.class);
        when(stripeSub.cancel(any(SubscriptionCancelParams.class))).thenReturn(cancelled);

        com.stripe.model.Subscription result = stripeService.cancelSubscription("sub_1", true);

        assertThat(result).isSameAs(cancelled);
        verify(stripeSub, never()).update(any(SubscriptionUpdateParams.class));
    }

    @Test
    @DisplayName("toCouponDuration : forever/once/repeating mapping")
    void couponDuration_mapping() {
        Coupon forever = mock(Coupon.class);
        when(forever.getDuration()).thenReturn("forever");
        Coupon once = mock(Coupon.class);
        when(once.getDuration()).thenReturn("once");
        Coupon repeating = mock(Coupon.class);
        when(repeating.getDuration()).thenReturn("repeating");
        Coupon unknown = mock(Coupon.class);
        when(unknown.getDuration()).thenReturn("weird");

        assertThat(StripeService.toCouponDuration(forever)).isEqualTo(CouponDuration.FOREVER);
        assertThat(StripeService.toCouponDuration(once)).isEqualTo(CouponDuration.ONCE);
        assertThat(StripeService.toCouponDuration(repeating)).isEqualTo(CouponDuration.MONTHS);
        assertThat(StripeService.toCouponDuration(unknown)).isNull();
        assertThat(StripeService.toCouponDuration(null)).isNull();
    }

    private Customer mockCustomer(String id) {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(id);
        customerMock.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenReturn(customer);
        return customer;
    }

    private com.stripe.model.Subscription mockStripeSub(String id, String status, Discount discount) {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn(id);
        when(stripeSub.getStatus()).thenReturn(status);
        when(stripeSub.getDiscount()).thenReturn(discount);
        Invoice invoice = mock(Invoice.class);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getClientSecret()).thenReturn(id + "_secret");
        when(invoice.getPaymentIntentObject()).thenReturn(intent);
        when(stripeSub.getLatestInvoiceObject()).thenReturn(invoice);
        return stripeSub;
    }
}
