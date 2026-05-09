package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.entity.ProcessedWebhookEvent;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeError;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private StripeConfig stripeConfig;
    @Mock private ProcessedWebhookEventRepository processedRepo;
    @Mock private SubscriptionService subscriptionService;

    private WebhookService webhookService;
    private MockedStatic<Webhook> webhookMock;

    private static final String EVENT_ID = "evt_test_1";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PI_ID = "pi_test_1";
    private static final String SECRET = "whsec_test";

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(stripeConfig, processedRepo, subscriptionService);
        webhookMock = mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        webhookMock.close();
    }

    private Event mockEvent(String type, Object stripeObject) {
        Event event = mock(Event.class);
        lenient().when(event.getId()).thenReturn(EVENT_ID);
        lenient().when(event.getType()).thenReturn(type);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        lenient().when(deserializer.getObject())
                .thenReturn(Optional.ofNullable((com.stripe.model.StripeObject) stripeObject));
        return event;
    }

    private PaymentIntent mockPaymentIntent(UUID userId, PlanTier plan) {
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn(PI_ID);
        when(pi.getMetadata()).thenReturn(Map.of(
                "userId", userId.toString(),
                "plan", plan.name()));
        return pi;
    }

    @Test
    @DisplayName("Signature invalide → PaymentException")
    void invalidSignature_throws() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenThrow(new SignatureVerificationException("bad sig", "sig"));

        assertThatThrownBy(() -> webhookService.processStripeWebhook("payload", "wrong-sig"))
                .isInstanceOf(PaymentException.class);

        verify(processedRepo, never()).save(any());
        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("Idempotence : event déjà traité, pas de re-processing")
    void alreadyProcessed_skip() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Event event = mockEvent("payment_intent.succeeded", null);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(true);

        webhookService.processStripeWebhook("payload", "sig");

        verifyNoInteractions(subscriptionService);
        verify(processedRepo, never()).save(any());
    }

    @Test
    @DisplayName("payment_intent.succeeded → activateAfterPayment + save processed event")
    void succeeded_activates() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        PaymentIntent pi = mockPaymentIntent(USER_ID, PlanTier.PREMIUM);
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(false);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                PaymentProvider.STRIPE, PI_ID);
        verify(processedRepo).save(argThat((ProcessedWebhookEvent saved) ->
                EVENT_ID.equals(saved.getEventId()) &&
                saved.getProvider() == PaymentProvider.STRIPE));
    }

    @Test
    @DisplayName("payment_intent.payment_failed → markTransactionFailed avec raison")
    void failed_marksFailed() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn(PI_ID);
        StripeError err = mock(StripeError.class);
        when(err.getMessage()).thenReturn("carte refusée");
        when(pi.getLastPaymentError()).thenReturn(err);
        Event event = mockEvent("payment_intent.payment_failed", pi);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(false);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).markTransactionFailed(PI_ID, "carte refusée");
        verify(processedRepo).save(any());
    }

    @Test
    @DisplayName("charge.refunded → markTransactionRefunded")
    void refunded_marksRefunded() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn(PI_ID);
        Event event = mockEvent("charge.refunded", charge);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(false);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService).markTransactionRefunded(PI_ID);
    }

    @Test
    @DisplayName("Event non géré : pas d'action mais marqué comme traité")
    void unknownEvent_recorded() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        Event event = mockEvent("customer.created", null);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(false);

        webhookService.processStripeWebhook("payload", "sig");

        verifyNoInteractions(subscriptionService);
        verify(processedRepo).save(any());
    }

    @Test
    @DisplayName("Metadata Stripe invalides → activate non appelé")
    void succeeded_invalidMetadata_skips() {
        when(stripeConfig.getStripeWebhookSecret()).thenReturn(SECRET);
        PaymentIntent pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn(PI_ID);
        when(pi.getMetadata()).thenReturn(Map.of("userId", "not-a-uuid", "plan", "BOGUS"));
        Event event = mockEvent("payment_intent.succeeded", pi);
        webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                .thenReturn(event);
        when(processedRepo.existsById(EVENT_ID)).thenReturn(false);

        webhookService.processStripeWebhook("payload", "sig");

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(processedRepo).save(any());
    }
}
