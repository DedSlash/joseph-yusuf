package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.CreateSubscriptionRequest;
import com.josephyusuf.subscription.dto.CreateSubscriptionResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.StripeSubscriptionResult;
import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.CouponDuration;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.mapper.SubscriptionMapper;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.josephyusuf.subscription.repository.TransactionRepository;
import com.stripe.model.Invoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SubscriptionMapper mapper;
    @Mock private AuthClient authClient;
    @Mock private StripeService stripeService;
    @Mock private StripeConfig stripeConfig;

    @InjectMocks private SubscriptionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EXTERNAL_TX = "pi_test_123";

    @Nested
    @DisplayName("createStripeSubscription")
    class CreateStripeSubscriptionTests {

        @Test
        @DisplayName("Crée subscription PENDING + délègue à StripeService")
        void createsPendingSubscription() {
            StripeSubscriptionResult result = StripeSubscriptionResult.builder()
                    .stripeSubscriptionId("sub_1").stripeCustomerId("cus_1")
                    .stripePriceId("price_1").clientSecret("pi_secret")
                    .status("incomplete").appliedCouponId("EARLY50")
                    .couponDuration(CouponDuration.FOREVER).build();
            when(stripeService.createSubscription(USER_ID, "u@e.com",
                    PlanTier.PREMIUM, "EUR", "EARLY50", "pm_x"))
                    .thenReturn(result);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateSubscriptionRequest req = CreateSubscriptionRequest.builder()
                    .planTier(PlanTier.PREMIUM).currency("EUR")
                    .paymentMethodId("pm_x").couponCode("EARLY50").build();
            CreateSubscriptionResponse resp = service.createStripeSubscription(USER_ID, "u@e.com", req);

            assertThat(resp.getSubscriptionId()).isEqualTo("sub_1");
            assertThat(resp.getClientSecret()).isEqualTo("pi_secret");
            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.PENDING
                            && "sub_1".equals(s.getStripeSubscriptionId())
                            && "EARLY50".equals(s.getStripeCouponId())
                            && CouponDuration.FOREVER == s.getCouponDuration()
                            && "EUR".equals(s.getCurrency())));
        }

        @Test
        @DisplayName("FREE → InvalidPlanException avant appel Stripe")
        void freePlanThrows() {
            CreateSubscriptionRequest req = CreateSubscriptionRequest.builder()
                    .planTier(PlanTier.FREE).currency("EUR").paymentMethodId("pm_x").build();
            assertThatThrownBy(() -> service.createStripeSubscription(USER_ID, "u@e.com", req))
                    .isInstanceOf(InvalidPlanException.class);
            verify(stripeService, never()).createSubscription(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("activateAfterPayment (manuel / Wave / Orange)")
    class ActivateTests {

        @Test
        @DisplayName("Crée une nouvelle subscription si absente")
        void activate_creates_new() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.of(
                    Transaction.builder().userId(USER_ID).status(TransactionStatus.PENDING).build()));

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                    PaymentProvider.STRIPE, EXTERNAL_TX);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            verify(authClient).updatePlan(any());
            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED));
        }

        @Test
        @DisplayName("Échec AuthClient swallowed")
        void activate_authClientFailure_swallowed() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("auth-service down")).when(authClient).updatePlan(any());

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                    PaymentProvider.STRIPE, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Webhooks-driven flows")
    class WebhookFlowsTests {

        @Test
        @DisplayName("activateSubscriptionFromInvoice → ACTIVE + transaction SUCCEEDED")
        void activateFromInvoice() {
            Subscription local = Subscription.builder().id(UUID.randomUUID()).userId(USER_ID)
                    .plan(PlanTier.PREMIUM).stripeSubscriptionId("sub_1").build();
            when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByStripeInvoiceId("inv_1")).thenReturn(Optional.empty());

            com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
            when(stripeSub.getStatus()).thenReturn("active");
            when(stripeSub.getCurrentPeriodEnd()).thenReturn(1900000000L);
            when(stripeSub.getCancelAtPeriodEnd()).thenReturn(false);
            when(stripeService.retrieveSubscription("sub_1")).thenReturn(stripeSub);

            Invoice invoice = mock(Invoice.class);
            when(invoice.getId()).thenReturn("inv_1");
            when(invoice.getSubscription()).thenReturn("sub_1");
            when(invoice.getAmountPaid()).thenReturn(499L);
            when(invoice.getCurrency()).thenReturn("eur");

            service.activateSubscriptionFromInvoice(invoice);

            verify(subscriptionRepository).save(argThat(s -> s.getStatus() == SubscriptionStatus.ACTIVE));
            verify(transactionRepository).save(argThat(tx ->
                    tx.getStatus() == TransactionStatus.SUCCEEDED
                            && "inv_1".equals(tx.getStripeInvoiceId())
                            && tx.getAmount().compareTo(new BigDecimal("499")) == 0));
            verify(authClient).updatePlan(any());
        }

        @Test
        @DisplayName("activateSubscriptionFromInvoice : subscription locale absente → no-op")
        void activateFromInvoice_unknownSub() {
            Invoice invoice = mock(Invoice.class);
            when(invoice.getSubscription()).thenReturn("sub_unknown");
            when(subscriptionRepository.findByStripeSubscriptionId("sub_unknown")).thenReturn(Optional.empty());

            service.activateSubscriptionFromInvoice(invoice);

            verify(subscriptionRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("recordPaymentFailure → transaction FAILED")
        void recordFailure() {
            Subscription local = Subscription.builder().id(UUID.randomUUID()).userId(USER_ID)
                    .plan(PlanTier.PREMIUM).stripeSubscriptionId("sub_1").build();
            when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(local));

            Invoice invoice = mock(Invoice.class);
            when(invoice.getId()).thenReturn("inv_fail");
            when(invoice.getSubscription()).thenReturn("sub_1");
            when(invoice.getAmountDue()).thenReturn(499L);
            when(invoice.getCurrency()).thenReturn("eur");

            service.recordPaymentFailure(invoice);

            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.FAILED
                    && "inv_fail".equals(tx.getStripeInvoiceId())));
        }

        @Test
        @DisplayName("downgradeToFree → CANCELLED + plan FREE + sync auth")
        void downgrade() {
            Subscription local = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .stripeCustomerId("cus_1").status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRepository.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.downgradeToFree("cus_1");

            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.CANCELLED
                            && s.getPlan() == PlanTier.FREE
                            && s.getCancelledAt() != null));
            verify(authClient).updatePlan(argThat(req -> req.getPlan() == PlanTier.FREE));
        }

        @Test
        @DisplayName("updateSubscriptionFromStripe → currentPeriodEnd mis à jour")
        void updateFromStripe() {
            Subscription local = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .stripeSubscriptionId("sub_1").build();
            when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
            when(stripeSub.getId()).thenReturn("sub_1");
            when(stripeSub.getStatus()).thenReturn("active");
            when(stripeSub.getCurrentPeriodEnd()).thenReturn(1900000000L);
            when(stripeSub.getCancelAtPeriodEnd()).thenReturn(true);

            service.updateSubscriptionFromStripe(stripeSub);

            verify(subscriptionRepository).save(argThat(s ->
                    s.getCurrentPeriodEnd() != null && s.isCancelAtPeriodEnd()));
        }
    }

    @Nested
    @DisplayName("cancelStripeSubscription / setAutoRenew")
    class CancelTests {

        @Test
        @DisplayName("cancelStripeSubscription immediately=false → cancelAtPeriodEnd")
        void cancel_atEnd() {
            Subscription local = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .stripeSubscriptionId("sub_1").status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().cancelAtPeriodEnd(true).build());

            com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
            when(stripeSub.getCancelAtPeriodEnd()).thenReturn(true);
            when(stripeService.cancelSubscription("sub_1", false)).thenReturn(stripeSub);

            SubscriptionResponse resp = service.cancelStripeSubscription(USER_ID, false);

            assertThat(resp.isCancelAtPeriodEnd()).isTrue();
            verify(stripeService).cancelSubscription("sub_1", false);
        }

        @Test
        @DisplayName("cancelStripeSubscription sans subscription Stripe → InvalidPlanException")
        void cancel_noStripeSub() {
            Subscription local = Subscription.builder().userId(USER_ID).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(local));
            assertThatThrownBy(() -> service.cancelStripeSubscription(USER_ID, false))
                    .isInstanceOf(InvalidPlanException.class);
        }

        @Test
        @DisplayName("cancel - pas d'abonnement → SubscriptionNotFoundException")
        void cancel_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.cancelStripeSubscription(USER_ID, false))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("setAutoRenew(false) → délègue à StripeService.cancelSubscription")
        void setAutoRenew_off() {
            Subscription sub = Subscription.builder().userId(USER_ID)
                    .stripeSubscriptionId("sub_1").autoRenew(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
            when(stripeSub.getCancelAtPeriodEnd()).thenReturn(true);
            when(stripeService.cancelSubscription("sub_1", false)).thenReturn(stripeSub);
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().autoRenew(false).build());

            SubscriptionResponse result = service.setAutoRenew(USER_ID, false);

            assertThat(result.isAutoRenew()).isFalse();
            verify(stripeService).cancelSubscription("sub_1", false);
        }
    }

    @Nested
    @DisplayName("getCurrent / getHistory / recordPendingTransaction")
    class QueryTests {

        @Test
        @DisplayName("getCurrent retourne la subscription mappée avec nextInvoiceAmount")
        void getCurrent_returnsMapped() {
            Subscription s = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .currency("EUR").build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(s));
            when(mapper.toResponse(s)).thenReturn(SubscriptionResponse.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM).build());
            when(stripeConfig.getPremiumPriceEur()).thenReturn(499L);

            SubscriptionResponse result = service.getCurrent(USER_ID);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
            assertThat(result.getNextInvoiceAmount()).isEqualByComparingTo("499");
            assertThat(result.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("getCurrent : SubscriptionNotFoundException si absent")
        void getCurrent_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getCurrent(USER_ID))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("getHistory mappe la page")
        void getHistory_mapsPage() {
            Transaction t = Transaction.builder().userId(USER_ID).build();
            Page<Transaction> page = new PageImpl<>(List.of(t));
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(page);
            when(mapper.toResponse(t)).thenReturn(TransactionResponse.builder().build());

            Page<TransactionResponse> result = service.getHistory(USER_ID, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("recordPendingTransaction crée tx PENDING")
        void recordPending_success() {
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM).provider(PaymentProvider.WAVE)
                    .externalTxId("wave_1").amount(new BigDecimal("3000")).currency("XOF").build();

            Transaction tx = service.recordPendingTransaction(params);

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getProvider()).isEqualTo(PaymentProvider.WAVE);
        }

        @Test
        @DisplayName("recordPendingTransaction FREE → InvalidPlanException")
        void recordPending_free() {
            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.FREE).provider(PaymentProvider.WAVE)
                    .externalTxId("wave_1").amount(BigDecimal.ZERO).currency("XOF").build();
            assertThatThrownBy(() -> service.recordPendingTransaction(params))
                    .isInstanceOf(InvalidPlanException.class);
        }
    }
}
