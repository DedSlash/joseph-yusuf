package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PaymentIntentResponse;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import com.stripe.exception.ApiException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock private StripeConfig stripeConfig;

    private StripeService stripeService;
    private MockedStatic<PaymentIntent> piMock;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(stripeConfig);
        piMock = mockStatic(PaymentIntent.class);
    }

    @AfterEach
    void tearDown() {
        piMock.close();
    }

    @Test
    @DisplayName("PREMIUM EUR → 499 cents, clientSecret retourné")
    void premium_eur_returnsResponse() {
        when(stripeConfig.getPremiumPriceEur()).thenReturn(499L);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_test_1");
        when(intent.getClientSecret()).thenReturn("pi_test_1_secret_xxx");
        when(intent.getStatus()).thenReturn("requires_payment_method");

        piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenReturn(intent);

        PaymentIntentResponse response = stripeService.createPaymentIntent(
                UUID.randomUUID(), PlanTier.PREMIUM, "EUR");

        assertThat(response.getPaymentIntentId()).isEqualTo("pi_test_1");
        assertThat(response.getClientSecret()).isEqualTo("pi_test_1_secret_xxx");
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getAmount()).isEqualByComparingTo("499");
    }

    @Test
    @DisplayName("PREMIUM_PLUS XOF → 600000")
    void premiumPlus_xof_returnsResponse() {
        when(stripeConfig.getPremiumPlusPriceXof()).thenReturn(600000L);
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_test_2");
        when(intent.getClientSecret()).thenReturn("secret");
        when(intent.getStatus()).thenReturn("requires_payment_method");
        piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenReturn(intent);

        PaymentIntentResponse response = stripeService.createPaymentIntent(
                UUID.randomUUID(), PlanTier.PREMIUM_PLUS, "XOF");

        assertThat(response.getAmount()).isEqualByComparingTo("600000");
        assertThat(response.getCurrency()).isEqualTo("XOF");
    }

    @Test
    @DisplayName("FREE → InvalidPlanException")
    void free_throws() {
        assertThatThrownBy(() -> stripeService.createPaymentIntent(
                UUID.randomUUID(), PlanTier.FREE, "EUR"))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("StripeException → PaymentException")
    void stripeException_wrapped() {
        when(stripeConfig.getPremiumPriceEur()).thenReturn(499L);
        piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenThrow(new ApiException("API down", "req_1", "api_error", 500, null));

        assertThatThrownBy(() -> stripeService.createPaymentIntent(
                UUID.randomUUID(), PlanTier.PREMIUM, "EUR"))
                .isInstanceOf(PaymentException.class);
    }
}
