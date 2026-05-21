package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.dto.PromoCodePublicValidationResponse;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.service.PaymentMethodConfigService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Validation publique code promo")
class PromoCodePublicValidationTest {

    @Mock
    private AdminClient adminClient;

    @Mock
    private PaymentMethodConfigService paymentMethodConfigService;

    @InjectMocks
    private PaymentMethodConfigController controller;

    @Test
    @DisplayName("Code valide → valid: true avec discountPercent")
    void validCode_returnsValid() {
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .id(UUID.randomUUID())
                .code("JOSEPH20")
                .discountPercent(20)
                .valid(true)
                .build();
        when(adminClient.validatePublic("JOSEPH20")).thenReturn(validation);

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("JOSEPH20");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isTrue();
        assertThat(response.getBody().getCode()).isEqualTo("JOSEPH20");
        assertThat(response.getBody().getDiscountPercent()).isEqualTo(20);
        assertThat(response.getBody().getReason()).isNull();
    }

    @Test
    @DisplayName("Code expiré → valid: false, reason: EXPIRED")
    void expiredCode_returnsExpired() {
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .valid(false)
                .reason("EXPIRED")
                .build();
        when(adminClient.validatePublic("OLD_CODE")).thenReturn(validation);

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("OLD_CODE");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getReason()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Code épuisé → valid: false, reason: MAX_USES_REACHED")
    void exhaustedCode_returnsMaxUsesReached() {
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .valid(false)
                .reason("MAX_USES_REACHED")
                .build();
        when(adminClient.validatePublic("FULL_CODE")).thenReturn(validation);

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("FULL_CODE");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getReason()).isEqualTo("MAX_USES_REACHED");
    }

    @Test
    @DisplayName("Code inexistant → valid: false, reason: NOT_FOUND")
    void unknownCode_returnsNotFound() {
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .valid(false)
                .reason("NOT_FOUND")
                .build();
        when(adminClient.validatePublic("NOPE")).thenReturn(validation);

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("NOPE");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getReason()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Code désactivé → valid: false, reason: NOT_FOUND")
    void disabledCode_returnsNotFound() {
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .valid(false)
                .reason("NOT_FOUND")
                .build();
        when(adminClient.validatePublic("DISABLED")).thenReturn(validation);

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("DISABLED");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getReason()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Erreur réseau Feign → valid: false, reason: NOT_FOUND")
    void networkError_returnsNotFound() {
        Request feignRequest = Request.create(Request.HttpMethod.GET, "/test",
                java.util.Collections.emptyMap(), null, new RequestTemplate());
        when(adminClient.validatePublic("NET_ERR"))
                .thenThrow(new FeignException.ServiceUnavailable("Connection refused", feignRequest, null, null));

        ResponseEntity<PromoCodePublicValidationResponse> response = controller.validatePromoPublic("NET_ERR");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getReason()).isEqualTo("NOT_FOUND");
    }
}
