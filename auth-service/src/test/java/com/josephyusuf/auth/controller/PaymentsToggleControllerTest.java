package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.client.dto.InternalAlertRequest;
import com.josephyusuf.auth.dto.PaymentsToggleActivateResponse;
import com.josephyusuf.auth.dto.PaymentsToggleStatusDto;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import com.josephyusuf.auth.service.EmailService;
import com.josephyusuf.auth.service.SystemSettingsService;
import com.josephyusuf.auth.service.TrialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentsToggleControllerTest {

    @Mock private SystemSettingsService systemSettingsService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private TrialService trialService;

    @InjectMocks
    private PaymentsToggleController controller;

    private User trialUser(LocalDateTime trialStartedAt) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("pwd")
                .firstName("Jane")
                .lastName("Doe")
                .plan(Plan.PREMIUM_PLUS)
                .role(Role.USER)
                .enabled(true)
                .country("SN")
                .currency("XOF")
                .inTrial(true)
                .trialUsed(true)
                .trialStartedAt(trialStartedAt)
                .trialEndsAt(trialStartedAt.plusDays(37))
                .build();
    }

    @Test
    @DisplayName("status → renvoie l'état + nombre d'utilisateurs en trial")
    void status_returnsState() {
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.countByInTrialTrue()).thenReturn(23L);

        ResponseEntity<PaymentsToggleStatusDto> response = controller.status();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().isPaymentsActive()).isFalse();
        assertThat(response.getBody().getUsersInTrialExtension()).isEqualTo(23L);
    }

    @Test
    @DisplayName("activate déjà actif → no-op + alreadyActive=true")
    void activate_alreadyActive() {
        when(systemSettingsService.isPaymentsActive()).thenReturn(true);

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        assertThat(response.getBody().isAlreadyActive()).isTrue();
        assertThat(response.getBody().getUsersNotified()).isZero();
        verify(systemSettingsService, never()).setPaymentsActive(anyBoolean());
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
        verify(emailService, never()).sendPaymentsActivatedGrace24h(any());
    }

    @Test
    @DisplayName("activate → user encore dans 7j initiaux : trialEndsAt remis à originalEnd, email trial actif")
    void activate_userWithinOriginalTrial() {
        LocalDateTime trialStart = LocalDateTime.now().minusDays(3);
        User user = trialUser(trialStart);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(systemSettingsService).setPaymentsActive(true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        // trialStarted + 7 jours = 4 jours dans le futur
        assertThat(saved.getTrialEndsAt())
                .isAfter(LocalDateTime.now().plusDays(3))
                .isBefore(LocalDateTime.now().plusDays(5));

        verify(emailService).sendPaymentsActivatedTrialActive(user);
        verify(emailService, never()).sendPaymentsActivatedGrace24h(any());

        verify(trialService).pushInAppAlert(
                eq(user.getId()), eq("PAYMENTS_ACTIVATED"), eq("INFO"), anyString(), anyString());

        assertThat(response.getBody().getUsersInOriginalTrial()).isEqualTo(1);
        assertThat(response.getBody().getUsersInGrace24h()).isZero();
        assertThat(response.getBody().getUsersNotified()).isEqualTo(1);
        assertThat(response.getBody().isAlreadyActive()).isFalse();
    }

    @Test
    @DisplayName("activate → user au-delà des 7j (trial étendu) : trialEndsAt = now + 24h, email grace")
    void activate_userBeyondOriginalTrial() {
        LocalDateTime trialStart = LocalDateTime.now().minusDays(20);
        User user = trialUser(trialStart);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(systemSettingsService).setPaymentsActive(true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getTrialEndsAt())
                .isAfter(LocalDateTime.now().plusHours(23))
                .isBefore(LocalDateTime.now().plusHours(25));

        verify(emailService).sendPaymentsActivatedGrace24h(user);
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());

        verify(trialService).pushInAppAlert(
                eq(user.getId()), eq("PAYMENTS_GRACE_24H"), eq("WARNING"), anyString(), anyString());

        assertThat(response.getBody().getUsersInGrace24h()).isEqualTo(1);
        assertThat(response.getBody().getUsersInOriginalTrial()).isZero();
    }

    @Test
    @DisplayName("activate → mix users dans/hors trial initial : compteurs séparés")
    void activate_mixedPopulation() {
        User insideTrial = trialUser(LocalDateTime.now().minusDays(2));
        User beyondTrial1 = trialUser(LocalDateTime.now().minusDays(15));
        User beyondTrial2 = trialUser(LocalDateTime.now().minusDays(30));
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(insideTrial, beyondTrial1, beyondTrial2));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(emailService, times(1)).sendPaymentsActivatedTrialActive(insideTrial);
        verify(emailService, times(1)).sendPaymentsActivatedGrace24h(beyondTrial1);
        verify(emailService, times(1)).sendPaymentsActivatedGrace24h(beyondTrial2);

        assertThat(response.getBody().getUsersInOriginalTrial()).isEqualTo(1);
        assertThat(response.getBody().getUsersInGrace24h()).isEqualTo(2);
        assertThat(response.getBody().getUsersNotified()).isEqualTo(3);
    }

    @Test
    @DisplayName("activate → trialStartedAt null : traité comme trial dépassé (grace 24h)")
    void activate_nullTrialStartedAt() {
        User userNoStart = trialUser(LocalDateTime.now().minusDays(5));
        userNoStart.setTrialStartedAt(null);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(userNoStart));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(emailService).sendPaymentsActivatedGrace24h(userNoStart);
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
        assertThat(response.getBody().getUsersInGrace24h()).isEqualTo(1);
    }

    @Test
    @DisplayName("activate → 0 utilisateurs en trial : flag basculé, aucun email")
    void activate_noTrialUsers() {
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of());

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(systemSettingsService).setPaymentsActive(true);
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
        verify(emailService, never()).sendPaymentsActivatedGrace24h(any());
        assertThat(response.getBody().getUsersNotified()).isZero();
        assertThat(response.getBody().isAlreadyActive()).isFalse();
    }
}
