package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private StripeConfig stripeConfig;
    @Mock private ProcessedWebhookEventRepository processedRepo;
    @Mock private SubscriptionService subscriptionService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private AdminClient adminClient;

    private WebhookService webhookService;
    private MockedStatic<Webhook> webhookMock;

    private static final String EVENT_ID = "evt_test_1";
    private static final String SECRET = "whsec_test";

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(stripeConfig, processedRepo, subscriptionService,
                subscriptionRepository, adminClient);
        webhookMock = mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        webhookMock.close();
    }

    private Event mockEvent(String type, com.stripe.model.StripeObject stripeObject) {
        Event event = mock(Event.class);
        lenient().when(event.getId()).thenReturn(EVENT_ID);
        lenient().when(event.getType()).thenReturn(type);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        lenient().when(deserializer.getObject()).thenReturn(Optional.ofNullable(stripeObject));
        return event;
    }

    @Test
    @DisplayName("invoice.payment_succeeded → activateSubscriptionFromInvoice")
    void invoicePaid() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Invoice invoice = mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_1");
        Event event = mockEvent("invoice.payment_succeeded", invoice);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);
        // pas de coupon : applyPromoCodeIfPresent ne fait rien
        when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.empty());

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).activateSubscriptionFromInvoice(invoice);
        verify(processedRepo).save(any());
    }

    @Test
    @DisplayName("invoice.payment_succeeded + couponApplied → adminClient.apply()")
    void invoicePaid_withCoupon() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Invoice invoice = mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_1");
        when(invoice.getId()).thenReturn("inv_1");
        Event event = mockEvent("invoice.payment_succeeded", invoice);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);

        UUID userId = UUID.randomUUID();
        Subscription local = Subscription.builder().userId(userId).stripeCouponId("EARLY50").build();
        when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(local));

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).activateSubscriptionFromInvoice(invoice);
        verify(adminClient).apply(argThat(req -> "EARLY50".equals(req.getCode())
                && userId.equals(req.getUserId())));
    }

    @Test
    @DisplayName("invoice.payment_failed → recordPaymentFailure")
    void invoiceFailed() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Invoice invoice = mock(Invoice.class);
        Event event = mockEvent("invoice.payment_failed", invoice);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).recordPaymentFailure(invoice);
    }

    @Test
    @DisplayName("customer.subscription.deleted → downgradeToFree")
    void subscriptionDeleted() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getCustomer()).thenReturn("cus_1");
        Event event = mockEvent("customer.subscription.deleted", stripeSub);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).downgradeToFree("cus_1");
    }

    @Test
    @DisplayName("customer.subscription.updated → updateSubscriptionFromStripe")
    void subscriptionUpdated() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        Event event = mockEvent("customer.subscription.updated", stripeSub);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).updateSubscriptionFromStripe(stripeSub);
    }

    @Test
    @DisplayName("Event déjà traité (idempotence) → no-op")
    void duplicateEvent_idempotent() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Event event = mockEvent("invoice.payment_succeeded", mock(Invoice.class));
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(true);

        webhookService.processStripeWebhook("payload", "sig");

        verifyNoInteractions(subscriptionService);
        verify(processedRepo, never()).save(any());
    }

    @Test
    @DisplayName("Événement inconnu → ignoré silencieusement, idempotence sauvegardée")
    void unknownEvent_ignored() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Event event = mockEvent("checkout.session.completed", null);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET))).thenReturn(event);

        webhookService.processStripeWebhook("payload", "sig");

        verifyNoInteractions(subscriptionService);
        verify(processedRepo).save(any());
    }

    @Test
    @DisplayName("Signature invalide → PaymentException")
    void invalidSignature_throws() throws Exception {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), eq(SECRET)))
                .thenThrow(new SignatureVerificationException("bad sig", "header"));

        assertThatThrownBy(() -> webhookService.processStripeWebhook("payload", "bad-sig"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Signature");
    }

}
