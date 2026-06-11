package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.dto.PaymentMethodConfigDto;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.service.PaymentMethodConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMethodConfigController")
class PaymentMethodConfigControllerTest {

    @Mock private PaymentMethodConfigService service;
    @Mock private AdminClient adminClient;
    @Mock private Authentication auth;

    @InjectMocks
    private PaymentMethodConfigController controller;

    @Test
    @DisplayName("getAvailable → renvoie la liste filtrée pour le client")
    void getAvailable() {
        List<PaymentMethodConfigDto> configs = List.of(
                PaymentMethodConfigDto.builder().provider(PaymentProvider.WAVE)
                        .enabled(true).displayName("Wave").displayOrder(1)
                        .paytechMethodCode("wave").build()
        );
        when(service.getAvailableForClient()).thenReturn(configs);

        ResponseEntity<List<PaymentMethodConfigDto>> response = controller.getAvailable();

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getProvider()).isEqualTo(PaymentProvider.WAVE);
        assertThat(response.getBody().get(0).getPaytechMethodCode()).isEqualTo("wave");
    }

    @Test
    @DisplayName("validatePromo → délègue à adminClient avec userId")
    void validatePromo() {
        UUID userId = UUID.randomUUID();
        when(auth.getPrincipal()).thenReturn(userId.toString());
        PromoCodeValidation validation = PromoCodeValidation.builder()
                .valid(true).code("TEST20").discountPercent(20).build();
        when(adminClient.validate("TEST20", userId)).thenReturn(validation);

        ResponseEntity<PromoCodeValidation> response = controller.validatePromo("TEST20", auth);

        assertThat(response.getBody().isValid()).isTrue();
        assertThat(response.getBody().getDiscountPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("getAllAdmin → renvoie tous les moyens (sans filtre)")
    void getAllAdmin() {
        List<PaymentMethodConfigDto> configs = List.of(
                PaymentMethodConfigDto.builder().provider(PaymentProvider.PAYTECH)
                        .enabled(false).displayName("PayTech").displayOrder(98).build()
        );
        when(service.getAll()).thenReturn(configs);

        ResponseEntity<List<PaymentMethodConfigDto>> response = controller.getAllAdmin();

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("toggle → bascule l'état du provider")
    void toggle() {
        PaymentMethodConfigDto dto = PaymentMethodConfigDto.builder()
                .provider(PaymentProvider.WAVE).enabled(false).build();
        when(service.toggle(PaymentProvider.WAVE)).thenReturn(dto);

        ResponseEntity<PaymentMethodConfigDto> response = controller.toggle(PaymentProvider.WAVE);

        assertThat(response.getBody().isEnabled()).isFalse();
    }
}
