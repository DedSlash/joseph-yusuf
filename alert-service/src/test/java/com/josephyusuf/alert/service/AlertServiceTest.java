package com.josephyusuf.alert.service;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.dto.RuleAppliedEvent;
import com.josephyusuf.alert.entity.Alert;
import com.josephyusuf.alert.entity.AlertSeverity;
import com.josephyusuf.alert.entity.AlertType;
import com.josephyusuf.alert.exception.AlertNotFoundException;
import com.josephyusuf.alert.exception.UnauthorizedAccessException;
import com.josephyusuf.alert.mapper.AlertMapper;
import com.josephyusuf.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private AlertService alertService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();

    private Alert alert;
    private AlertDto alertDto;

    @BeforeEach
    void setUp() {
        alert = Alert.builder()
                .id(ALERT_ID)
                .userId(USER_ID)
                .type(AlertType.INFO)
                .severity(AlertSeverity.INFO)
                .title("Titre")
                .message("Message")
                .read(false)
                .build();

        alertDto = AlertDto.builder()
                .id(ALERT_ID)
                .userId(USER_ID)
                .type(AlertType.INFO)
                .severity(AlertSeverity.INFO)
                .title("Titre")
                .message("Message")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("listByUser")
    class ListByUserTests {

        @Test
        @DisplayName("retourne la liste des alertes mappees")
        void returnsMappedAlerts() {
            when(alertRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(alert));
            when(alertMapper.toDto(alert)).thenReturn(alertDto);

            List<AlertDto> result = alertService.listByUser(USER_ID);

            assertThat(result).hasSize(1).containsExactly(alertDto);
        }

        @Test
        @DisplayName("retourne liste vide quand aucun resultat")
        void returnsEmptyWhenNoAlerts() {
            when(alertRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

            List<AlertDto> result = alertService.listByUser(USER_ID);

            assertThat(result).isEmpty();
            verify(alertMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("listUnread")
    class ListUnreadTests {

        @Test
        @DisplayName("retourne uniquement les non lues")
        void returnsOnlyUnread() {
            when(alertRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(alert));
            when(alertMapper.toDto(alert)).thenReturn(alertDto);

            List<AlertDto> result = alertService.listUnread(USER_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("countUnread")
    class CountUnreadTests {

        @Test
        @DisplayName("retourne le compte du repository")
        void returnsRepositoryCount() {
            when(alertRepository.countByUserIdAndReadFalse(USER_ID)).thenReturn(5L);

            assertThat(alertService.countUnread(USER_ID)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("marque comme lue et retourne le dto")
        void marksAsReadAndReturnsDto() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));
            when(alertRepository.save(alert)).thenReturn(alert);
            when(alertMapper.toDto(alert)).thenReturn(alertDto);

            AlertDto result = alertService.markAsRead(USER_ID, ALERT_ID);

            assertThat(result).isEqualTo(alertDto);
            assertThat(alert.isRead()).isTrue();
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("leve AlertNotFoundException si alerte inexistante")
        void throwsWhenAlertNotFound() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.markAsRead(USER_ID, ALERT_ID))
                    .isInstanceOf(AlertNotFoundException.class)
                    .hasMessageContaining("introuvable");
        }

        @Test
        @DisplayName("leve UnauthorizedAccessException si l'utilisateur n'est pas proprietaire")
        void throwsWhenNotOwner() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> alertService.markAsRead(OTHER_USER_ID, ALERT_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("marque toutes les alertes non lues comme lues")
        void marksAllUnreadAsRead() {
            Alert a1 = Alert.builder().id(UUID.randomUUID()).userId(USER_ID).read(false).build();
            Alert a2 = Alert.builder().id(UUID.randomUUID()).userId(USER_ID).read(false).build();
            when(alertRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(a1, a2));

            alertService.markAllAsRead(USER_ID);

            assertThat(a1.isRead()).isTrue();
            assertThat(a2.isRead()).isTrue();
            verify(alertRepository).saveAll(List.of(a1, a2));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("supprime l'alerte du proprietaire")
        void deletesOwnedAlert() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            alertService.delete(USER_ID, ALERT_ID);

            verify(alertRepository).delete(alert);
        }

        @Test
        @DisplayName("refuse la suppression d'une alerte d'un autre utilisateur")
        void refusesDeleteWhenNotOwner() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(alert));

            assertThatThrownBy(() -> alertService.delete(OTHER_USER_ID, ALERT_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(alertRepository, never()).delete(any(Alert.class));
        }

        @Test
        @DisplayName("leve AlertNotFoundException si l'alerte n'existe pas")
        void throwsWhenNotFound() {
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.delete(USER_ID, ALERT_ID))
                    .isInstanceOf(AlertNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createFromIncomeClassified")
    class CreateFromIncomeClassifiedTests {

        @Test
        @DisplayName("ABUNDANCE genere une alerte SUCCESS")
        void abundanceGeneratesSuccessAlert() {
            IncomeClassifiedEvent event = IncomeClassifiedEvent.builder()
                    .userId(USER_ID)
                    .month(5)
                    .year(2026)
                    .totalIncome(new BigDecimal("3000"))
                    .averageLast3Months(new BigDecimal("2000"))
                    .status("ABUNDANCE")
                    .percentageVsAverage(50.0)
                    .occurredAt(Instant.now())
                    .build();

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            when(alertRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            Alert created = alertService.createFromIncomeClassified(event);

            assertThat(created).isNotNull();
            Alert saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(AlertType.ABUNDANCE_DETECTED);
            assertThat(saved.getSeverity()).isEqualTo(AlertSeverity.SUCCESS);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getMonth()).isEqualTo(5);
            assertThat(saved.getYear()).isEqualTo(2026);
            assertThat(saved.isRead()).isFalse();
            assertThat(saved.getTitle()).contains("abondance");
        }

        @Test
        @DisplayName("LEAN genere une alerte WARNING")
        void leanGeneratesWarningAlert() {
            IncomeClassifiedEvent event = IncomeClassifiedEvent.builder()
                    .userId(USER_ID)
                    .month(6)
                    .year(2026)
                    .totalIncome(new BigDecimal("1000"))
                    .averageLast3Months(new BigDecimal("2000"))
                    .status("LEAN")
                    .percentageVsAverage(-50.0)
                    .occurredAt(Instant.now())
                    .build();

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            when(alertRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            Alert created = alertService.createFromIncomeClassified(event);

            assertThat(created).isNotNull();
            Alert saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(AlertType.LEAN_DETECTED);
            assertThat(saved.getSeverity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(saved.getTitle()).contains("disette");
        }

        @Test
        @DisplayName("NORMAL ne cree aucune alerte")
        void normalDoesNotCreateAlert() {
            IncomeClassifiedEvent event = IncomeClassifiedEvent.builder()
                    .userId(USER_ID)
                    .month(7)
                    .year(2026)
                    .totalIncome(new BigDecimal("2000"))
                    .averageLast3Months(new BigDecimal("2000"))
                    .status("NORMAL")
                    .percentageVsAverage(0.0)
                    .occurredAt(Instant.now())
                    .build();

            Alert created = alertService.createFromIncomeClassified(event);

            assertThat(created).isNull();
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createFromRuleApplied")
    class CreateFromRuleAppliedTests {

        @Test
        @DisplayName("genere une alerte INFO de type RULE_APPLIED")
        void generatesInfoAlert() {
            RuleAppliedEvent event = RuleAppliedEvent.builder()
                    .userId(USER_ID)
                    .rule("RULE_50_30_20")
                    .totalIncome(new BigDecimal("2500"))
                    .monthStatus("NORMAL")
                    .month(8)
                    .year(2026)
                    .occurredAt(Instant.now())
                    .build();

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            when(alertRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            Alert created = alertService.createFromRuleApplied(event);

            assertThat(created).isNotNull();
            Alert saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(AlertType.RULE_APPLIED);
            assertThat(saved.getSeverity()).isEqualTo(AlertSeverity.INFO);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getMonth()).isEqualTo(8);
            assertThat(saved.getYear()).isEqualTo(2026);
            assertThat(saved.getTitle()).contains("50/30/20");
            assertThat(saved.getMessage()).contains("2 500");
        }
    }
}
