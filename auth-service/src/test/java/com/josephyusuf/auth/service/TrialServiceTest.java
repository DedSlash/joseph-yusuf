package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TrialService trialService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("trial@example.com")
                .password("encoded")
                .firstName("Joseph")
                .lastName("Yusuf")
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .country("SN")
                .currency("XOF")
                .trialUsed(false)
                .inTrial(false)
                .build();
    }

    @Test
    @DisplayName("startTrial - active PREMIUM_PLUS + inTrial=true + trialUsed=true + email envoyé")
    void startTrial_nominal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        trialService.startTrial(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getPlan()).isEqualTo(Plan.PREMIUM_PLUS);
        assertThat(saved.isInTrial()).isTrue();
        assertThat(saved.isTrialUsed()).isTrue();
        assertThat(saved.getTrialStartedAt()).isNotNull();
        assertThat(saved.getTrialEndsAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(saved.getTrialEndsAt()).isBefore(LocalDateTime.now().plusDays(8));

        verify(emailService).sendTrialWelcome(saved);
    }

    @Test
    @DisplayName("startTrial - deuxième appel ignoré si trial déjà utilisé")
    void startTrial_alreadyUsed() {
        user.setTrialUsed(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        trialService.startTrial(userId);

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendTrialWelcome(any(User.class));
    }

    @Test
    @DisplayName("expireTrials - downgrade FREE + email + inTrial=false")
    void expireTrials_downgradesToFree() {
        User expired = User.builder()
                .id(userId)
                .email("trial@example.com")
                .password("p")
                .firstName("J")
                .lastName("Y")
                .plan(Plan.PREMIUM_PLUS)
                .role(Role.USER)
                .enabled(true)
                .country("SN")
                .currency("XOF")
                .inTrial(true)
                .trialUsed(true)
                .trialEndsAt(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByInTrialTrueAndTrialEndsAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        trialService.expireTrials();

        assertThat(expired.getPlan()).isEqualTo(Plan.FREE);
        assertThat(expired.isInTrial()).isFalse();
        verify(userRepository).save(expired);
        verify(emailService).sendTrialExpired(expired);
    }

    @Test
    @DisplayName("expireTrials - aucun utilisateur trial expiré → no-op")
    void expireTrials_noExpired() {
        when(userRepository.findByInTrialTrueAndTrialEndsAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        trialService.expireTrials();

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendTrialExpired(any(User.class));
    }

    @Test
    @DisplayName("sendTrialReminders - envoi à J-1 pour chaque user")
    void sendTrialReminders_nominal() {
        User u1 = User.builder().id(UUID.randomUUID()).email("a@b.c").firstName("A").lastName("B")
                .password("p").plan(Plan.PREMIUM_PLUS).role(Role.USER).enabled(true)
                .country("SN").currency("XOF").inTrial(true).trialUsed(true)
                .trialEndsAt(LocalDateTime.now().plusHours(25)).build();
        User u2 = User.builder().id(UUID.randomUUID()).email("c@d.e").firstName("C").lastName("D")
                .password("p").plan(Plan.PREMIUM_PLUS).role(Role.USER).enabled(true)
                .country("SN").currency("XOF").inTrial(true).trialUsed(true)
                .trialEndsAt(LocalDateTime.now().plusHours(30)).build();

        when(userRepository.findByInTrialTrueAndTrialEndsAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(u1, u2));

        trialService.sendTrialReminders();

        verify(emailService).sendTrialReminder(u1);
        verify(emailService).sendTrialReminder(u2);
    }

    @Test
    @DisplayName("getTrialStatus - utilisateur en trial → isInTrial=true + jours restants")
    void getTrialStatus_inTrial() {
        user.setInTrial(true);
        user.setTrialUsed(true);
        user.setTrialEndsAt(LocalDateTime.now().plusDays(5));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TrialService.TrialStatus status = trialService.getTrialStatus(userId);

        assertThat(status.isInTrial()).isTrue();
        assertThat(status.daysRemaining()).isBetween(4, 5);
        assertThat(status.trialUsed()).isTrue();
        assertThat(status.trialEndsAt()).isNotNull();
    }

    @Test
    @DisplayName("getTrialStatus - utilisateur hors trial → isInTrial=false")
    void getTrialStatus_notInTrial() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TrialService.TrialStatus status = trialService.getTrialStatus(userId);

        assertThat(status.isInTrial()).isFalse();
        assertThat(status.daysRemaining()).isZero();
        assertThat(status.trialEndsAt()).isNull();
    }
}
