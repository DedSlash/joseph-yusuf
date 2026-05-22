package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.WaitlistJoinRequest;
import com.josephyusuf.auth.dto.WaitlistJoinResponse;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.WaitlistEntry;
import com.josephyusuf.auth.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private WaitlistService waitlistService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waitlistService, "reservedPromoCode", "EARLY50");
    }

    @Nested
    @DisplayName("join")
    class JoinTests {

        @Test
        @DisplayName("Nouvelle inscription : sauvegarde, envoie email, retourne alreadyRegistered=false")
        void newEntry_isPersistedAndEmailed() {
            WaitlistJoinRequest request = WaitlistJoinRequest.builder()
                    .email("USER@example.com")
                    .planTier(Plan.PREMIUM)
                    .country("SN")
                    .currency("XOF")
                    .build();

            when(waitlistRepository.findByEmailAndPlanTier("user@example.com", Plan.PREMIUM))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<WaitlistEntry> captor = ArgumentCaptor.forClass(WaitlistEntry.class);
            when(waitlistRepository.save(captor.capture()))
                    .thenAnswer(invocation -> {
                        WaitlistEntry e = captor.getValue();
                        e.setId(UUID.randomUUID());
                        e.setCreatedAt(Instant.now());
                        e.setUpdatedAt(Instant.now());
                        return e;
                    });

            WaitlistJoinResponse response = waitlistService.join(request);

            WaitlistEntry saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("user@example.com");
            assertThat(saved.getPlanTier()).isEqualTo(Plan.PREMIUM);
            assertThat(saved.getCountry()).isEqualTo("SN");
            assertThat(saved.getCurrency()).isEqualTo("XOF");
            assertThat(saved.getPromoCodeReserved()).isEqualTo("EARLY50");
            assertThat(saved.isNotified()).isFalse();

            assertThat(response.isAlreadyRegistered()).isFalse();
            assertThat(response.getEmail()).isEqualTo("user@example.com");
            assertThat(response.getPlanTier()).isEqualTo("PREMIUM");
            assertThat(response.getPromoCodeReserved()).isEqualTo("EARLY50");
            assertThat(response.getMessage()).contains("Inscription confirmée");

            verify(emailService, times(1))
                    .sendWaitlistConfirmationEmail("user@example.com", "PREMIUM", "EARLY50");
        }

        @Test
        @DisplayName("Email déjà inscrit pour ce plan : alreadyRegistered=true, pas de save ni d'email")
        void duplicateEntry_returnsAlreadyRegisteredAndSkipsEmail() {
            WaitlistJoinRequest request = WaitlistJoinRequest.builder()
                    .email("existing@example.com")
                    .planTier(Plan.PREMIUM_PLUS)
                    .build();

            WaitlistEntry existing = WaitlistEntry.builder()
                    .id(UUID.randomUUID())
                    .email("existing@example.com")
                    .planTier(Plan.PREMIUM_PLUS)
                    .promoCodeReserved("EARLY50")
                    .build();

            when(waitlistRepository.findByEmailAndPlanTier("existing@example.com", Plan.PREMIUM_PLUS))
                    .thenReturn(Optional.of(existing));

            WaitlistJoinResponse response = waitlistService.join(request);

            assertThat(response.isAlreadyRegistered()).isTrue();
            assertThat(response.getEmail()).isEqualTo("existing@example.com");
            assertThat(response.getPlanTier()).isEqualTo("PREMIUM_PLUS");
            assertThat(response.getPromoCodeReserved()).isEqualTo("EARLY50");
            assertThat(response.getMessage()).contains("déjà sur la liste d'attente");

            verify(waitlistRepository, never()).save(any());
            verify(emailService, never()).sendWaitlistConfirmationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Email normalisé en minuscules + trim avant lookup")
        void emailIsNormalizedBeforeLookup() {
            WaitlistJoinRequest request = WaitlistJoinRequest.builder()
                    .email("  USER@Example.COM  ")
                    .planTier(Plan.PREMIUM)
                    .build();

            when(waitlistRepository.findByEmailAndPlanTier("user@example.com", Plan.PREMIUM))
                    .thenReturn(Optional.empty());
            when(waitlistRepository.save(any())).thenAnswer(inv -> {
                WaitlistEntry e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            waitlistService.join(request);

            verify(waitlistRepository).findByEmailAndPlanTier("user@example.com", Plan.PREMIUM);
        }

        @Test
        @DisplayName("Country/currency par défaut SN/XOF si null dans la requête")
        void defaultCountryCurrencyApplied() {
            WaitlistJoinRequest request = WaitlistJoinRequest.builder()
                    .email("nodefault@example.com")
                    .planTier(Plan.PREMIUM)
                    .build();

            when(waitlistRepository.findByEmailAndPlanTier(anyString(), any()))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<WaitlistEntry> captor = ArgumentCaptor.forClass(WaitlistEntry.class);
            when(waitlistRepository.save(captor.capture())).thenAnswer(inv -> captor.getValue());

            waitlistService.join(request);

            assertThat(captor.getValue().getCountry()).isEqualTo("SN");
            assertThat(captor.getValue().getCurrency()).isEqualTo("XOF");
        }
    }

    @Nested
    @DisplayName("countByPlan")
    class CountTests {

        @Test
        @DisplayName("Délègue au repository")
        void delegatesToRepository() {
            when(waitlistRepository.countByPlanTier(Plan.PREMIUM)).thenReturn(42L);

            long count = waitlistService.countByPlan(Plan.PREMIUM);

            assertThat(count).isEqualTo(42L);
            verify(waitlistRepository).countByPlanTier(Plan.PREMIUM);
        }
    }
}
