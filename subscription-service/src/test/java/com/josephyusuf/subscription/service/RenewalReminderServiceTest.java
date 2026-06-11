package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AlertClient;
import com.josephyusuf.subscription.client.InternalAuthClient;
import com.josephyusuf.subscription.dto.InternalAlertRequest;
import com.josephyusuf.subscription.dto.RenewalReminderEmailRequest;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.RenewalReminderType;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.repository.RenewalReminderRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RenewalReminderServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private RenewalReminderRepository renewalReminderRepository;
    @Mock private RenewalReminderPersister persister;
    @Mock private InternalAuthClient internalAuthClient;
    @Mock private AlertClient alertClient;

    @InjectMocks private RenewalReminderService service;

    private Subscription expiringSub(Instant expiresAt, String coupon, boolean lifetime) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .plan(PlanTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(Instant.now().minus(27, ChronoUnit.DAYS))
                .expiresAt(expiresAt)
                .autoRenew(true)
                .couponApplied(coupon)
                .couponLifetime(lifetime)
                .build();
    }

    @Test
    @DisplayName("J-3 nominal : envoie email + alerte + persiste la trace")
    void jMinus3_sendsBoth() {
        Instant expiresAt = Instant.now().plus(3, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS);
        Subscription sub = expiringSub(expiresAt, "EARLY50", true);

        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(sub))
                .thenReturn(List.of());

        service.sendDailyReminders();

        ArgumentCaptor<RenewalReminderEmailRequest> emailCaptor =
                ArgumentCaptor.forClass(RenewalReminderEmailRequest.class);
        verify(internalAuthClient).sendRenewalReminderEmail(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getType()).isEqualTo(RenewalReminderType.J_MINUS_3);
        assertThat(emailCaptor.getValue().getCouponApplied()).isEqualTo("EARLY50");
        assertThat(emailCaptor.getValue().isCouponLifetime()).isTrue();

        ArgumentCaptor<InternalAlertRequest> alertCaptor =
                ArgumentCaptor.forClass(InternalAlertRequest.class);
        verify(alertClient).createInternalAlert(alertCaptor.capture());
        assertThat(alertCaptor.getValue().getType()).isEqualTo("RENEWAL_REMINDER");
        assertThat(alertCaptor.getValue().getSeverity()).isEqualTo("INFO");

        verify(persister).recordSent(sub, RenewalReminderType.J_MINUS_3, expiresAt);
        verify(persister, never()).markExpired(any());
    }

    @Test
    @DisplayName("J-1 : severity WARNING dans l'alerte")
    void jMinus1_severityWarning() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS);
        Subscription sub = expiringSub(expiresAt, null, false);

        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(sub));

        service.sendDailyReminders();

        ArgumentCaptor<InternalAlertRequest> captor = ArgumentCaptor.forClass(InternalAlertRequest.class);
        verify(alertClient).createInternalAlert(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo("WARNING");
        verify(persister).recordSent(sub, RenewalReminderType.J_MINUS_1, expiresAt);
    }

    @Test
    @DisplayName("EXPIRED : envoie email + alerte + marque subscription EXPIRED")
    void expired_marksSubscriptionExpired() {
        Instant expiredAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Subscription sub = expiringSub(expiredAt, "EARLY50", true);

        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.findActiveExpiredBefore(eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of(sub));

        service.sendDailyReminders();

        ArgumentCaptor<RenewalReminderEmailRequest> captor =
                ArgumentCaptor.forClass(RenewalReminderEmailRequest.class);
        verify(internalAuthClient).sendRenewalReminderEmail(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RenewalReminderType.EXPIRED);
        verify(persister).recordSent(sub, RenewalReminderType.EXPIRED, expiredAt);
        verify(persister).markExpired(sub.getId());
    }

    @Test
    @DisplayName("Dédup : skip si rappel déjà envoyé pour cette période")
    void deduplicates_alreadySent() {
        Instant expiresAt = Instant.now().plus(3, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS);
        Subscription sub = expiringSub(expiresAt, null, false);

        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(sub))
                .thenReturn(List.of());
        when(renewalReminderRepository.existsBySubscriptionIdAndReminderTypeAndPeriodEndAt(
                sub.getId(), RenewalReminderType.J_MINUS_3, expiresAt))
                .thenReturn(true);

        service.sendDailyReminders();

        verify(internalAuthClient, never()).sendRenewalReminderEmail(any());
        verify(alertClient, never()).createInternalAlert(any());
        verify(persister, never()).recordSent(any(), any(), any());
    }

    @Test
    @DisplayName("Échec envoi email : ne persiste pas la trace (retry au prochain cron)")
    void emailFailure_skipsPersistence() {
        Instant expiresAt = Instant.now().plus(3, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS);
        Subscription sub = expiringSub(expiresAt, null, false);

        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(sub))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(internalAuthClient).sendRenewalReminderEmail(any());

        service.sendDailyReminders();

        verify(persister, never()).recordSent(any(), any(), any());
    }

    @Test
    @DisplayName("Aucune sub à notifier : aucun appel Feign")
    void noSubscriptions_noCalls() {
        when(subscriptionRepository.findActiveExpiringBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenReturn(List.of());

        service.sendDailyReminders();

        verify(internalAuthClient, never()).sendRenewalReminderEmail(any());
        verify(alertClient, never()).createInternalAlert(any());
    }
}
