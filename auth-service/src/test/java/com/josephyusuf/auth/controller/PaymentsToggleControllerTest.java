package com.josephyusuf.auth.controller;

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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

    private User trialUser(Instant createdAt) {
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
                .createdAt(createdAt)
                .trialStartedAt(LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC))
                .trialEndsAt(LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC).plusDays(37))
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
    @DisplayName("activate → user inscrit il y a 3j (encore dans 7j) : trialEndsAt = createdAt + 7j, email trial actif")
    void activate_userWithinSevenDaysOfSignup() {
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User user = trialUser(createdAt);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(systemSettingsService).setPaymentsActive(true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        // createdAt + 7 jours → 4 jours dans le futur
        assertThat(saved.getTrialEndsAt())
                .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusDays(3))
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).plusDays(5));

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
    @DisplayName("activate → user inscrit il y a 20j (trial étendu) : trialEndsAt = now + 24h, email grace")
    void activate_userBeyondSevenDaysSinceSignup() {
        Instant createdAt = Instant.now().minus(20, ChronoUnit.DAYS);
        User user = trialUser(createdAt);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(systemSettingsService).setPaymentsActive(true);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getTrialEndsAt())
                .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(23))
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).plusHours(25));

        verify(emailService).sendPaymentsActivatedGrace24h(user);
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());

        verify(trialService).pushInAppAlert(
                eq(user.getId()), eq("PAYMENTS_GRACE_24H"), eq("WARNING"), anyString(), anyString());

        assertThat(response.getBody().getUsersInGrace24h()).isEqualTo(1);
        assertThat(response.getBody().getUsersInOriginalTrial()).isZero();
    }

    @Test
    @DisplayName("activate → user inscrit il y a exactement 7j+1h : passe en grace 24h")
    void activate_userJustBeyondSevenDays() {
        Instant createdAt = Instant.now().minus(7, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS);
        User user = trialUser(createdAt);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        controller.activate();

        verify(emailService).sendPaymentsActivatedGrace24h(user);
        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
    }

    @Test
    @DisplayName("activate → user inscrit il y a 6j23h : reste protégé jusqu'à day 7")
    void activate_userOneHourBeforeSevenDays() {
        Instant createdAt = Instant.now().minus(6, ChronoUnit.DAYS).minus(23, ChronoUnit.HOURS);
        User user = trialUser(createdAt);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(user));

        controller.activate();

        verify(emailService).sendPaymentsActivatedTrialActive(user);
        verify(emailService, never()).sendPaymentsActivatedGrace24h(any());
    }

    @Test
    @DisplayName("activate → mix users dans/hors trial initial : compteurs séparés")
    void activate_mixedPopulation() {
        User insideTrial = trialUser(Instant.now().minus(2, ChronoUnit.DAYS));
        User beyondTrial1 = trialUser(Instant.now().minus(15, ChronoUnit.DAYS));
        User beyondTrial2 = trialUser(Instant.now().minus(30, ChronoUnit.DAYS));
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
    @DisplayName("activate → createdAt null (cas extrême) : traité comme trial dépassé")
    void activate_nullCreatedAt() {
        User userNoCreatedAt = trialUser(Instant.now().minus(5, ChronoUnit.DAYS));
        userNoCreatedAt.setCreatedAt(null);
        when(systemSettingsService.isPaymentsActive()).thenReturn(false);
        when(userRepository.findByInTrialTrue()).thenReturn(List.of(userNoCreatedAt));

        ResponseEntity<PaymentsToggleActivateResponse> response = controller.activate();

        verify(emailService).sendPaymentsActivatedGrace24h(userNoCreatedAt);
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

    @Test
    @DisplayName("previewEmail trial-active → envoie l'email avec firstName par défaut Admin")
    void previewEmail_trialActive_defaultFirstName() {
        ResponseEntity<Map<String, Object>> response = controller.previewEmail(
                "trial-active", "admin@josephyusuf.com", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("sent", true);
        assertThat(response.getBody()).containsEntry("template", "trial-active");
        assertThat(response.getBody()).containsEntry("to", "admin@josephyusuf.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendPaymentsActivatedTrialActive(userCaptor.capture());
        User fake = userCaptor.getValue();
        assertThat(fake.getEmail()).isEqualTo("admin@josephyusuf.com");
        assertThat(fake.getFirstName()).isEqualTo("Admin");
        assertThat(fake.getTrialEndsAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusDays(4));
    }

    @Test
    @DisplayName("previewEmail grace-24h → envoie l'email avec firstName fourni")
    void previewEmail_grace24h_customFirstName() {
        ResponseEntity<Map<String, Object>> response = controller.previewEmail(
                "grace-24h", "admin@josephyusuf.com", "Rey");

        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendPaymentsActivatedGrace24h(userCaptor.capture());
        User fake = userCaptor.getValue();
        assertThat(fake.getEmail()).isEqualTo("admin@josephyusuf.com");
        assertThat(fake.getFirstName()).isEqualTo("Rey");

        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
    }

    @Test
    @DisplayName("previewEmail template inconnu → 400 + supported list")
    void previewEmail_unknownTemplate() {
        ResponseEntity<Map<String, Object>> response = controller.previewEmail(
                "unknown", "admin@josephyusuf.com", null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody()).containsKey("supported");

        verify(emailService, never()).sendPaymentsActivatedTrialActive(any());
        verify(emailService, never()).sendPaymentsActivatedGrace24h(any());
    }

    @Test
    @DisplayName("previewEmail firstName vide → fallback Admin")
    void previewEmail_blankFirstName_fallback() {
        controller.previewEmail("grace-24h", "admin@josephyusuf.com", "   ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendPaymentsActivatedGrace24h(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("previewEmail n'écrit pas en DB")
    void previewEmail_noPersistence() {
        controller.previewEmail("trial-active", "admin@josephyusuf.com", null);

        verify(userRepository, never()).save(any(User.class));
        verify(systemSettingsService, never()).setPaymentsActive(anyBoolean());
    }
}
