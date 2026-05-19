package com.josephyusuf.income.savings.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.exception.UnauthorizedAccessException;
import com.josephyusuf.income.savings.dto.*;
import com.josephyusuf.income.savings.entity.SavingsContribution;
import com.josephyusuf.income.savings.entity.SavingsContributionType;
import com.josephyusuf.income.savings.entity.SavingsGoal;
import com.josephyusuf.income.savings.entity.SavingsGoalStatus;
import com.josephyusuf.income.savings.exception.InvalidSavingsGoalException;
import com.josephyusuf.income.savings.exception.SavingsGoalLimitExceededException;
import com.josephyusuf.income.savings.exception.SavingsGoalNotFoundException;
import com.josephyusuf.income.savings.repository.SavingsContributionRepository;
import com.josephyusuf.income.savings.repository.SavingsGoalRepository;
import com.josephyusuf.income.service.MonthSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository goalRepository;

    @Mock
    private SavingsContributionRepository contributionRepository;

    @Mock
    private SavingsMapper savingsMapper;

    @Mock
    private SavingsRecommendationService recommendationService;

    @Mock
    private MonthSummaryService monthSummaryService;

    @InjectMocks
    private SavingsGoalService goalService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GOAL_ID = UUID.randomUUID();

    private SavingsGoal sampleGoal(BigDecimal target, BigDecimal current) {
        return SavingsGoal.builder()
                .id(GOAL_ID)
                .userId(USER_ID)
                .name("Fonds d'urgence")
                .targetAmount(target)
                .currentAmount(current)
                .monthlyTarget(new BigDecimal("50000"))
                .startDate(LocalDate.now().minusMonths(1))
                .status(SavingsGoalStatus.ACTIVE)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("createGoal")
    class CreateTests {

        @Test
        @DisplayName("Création réussie")
        void create_success() {
            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("500000"))
                    .monthlyTarget(new BigDecimal("50000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .targetDate(LocalDate.of(2026, 12, 31))
                    .build();

            SavingsGoal saved = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            when(goalRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(0L);
            when(goalRepository.save(any(SavingsGoal.class))).thenReturn(saved);
            when(savingsMapper.toGoalDto(saved)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());
            when(recommendationService.computeProgressPercent(saved)).thenReturn(BigDecimal.ZERO);

            SavingsGoalDto dto = goalService.createGoal(USER_ID, "PREMIUM", request);

            assertThat(dto.getId()).isEqualTo(GOAL_ID);
            verify(goalRepository).save(any(SavingsGoal.class));
        }

        @Test
        @DisplayName("Plan FREE ne peut pas dépasser 1 objectif")
        void create_freePlanLimit() {
            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("500000"))
                    .monthlyTarget(new BigDecimal("50000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .build();

            when(goalRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(1L);

            assertThatThrownBy(() -> goalService.createGoal(USER_ID, "FREE", request))
                    .isInstanceOf(SavingsGoalLimitExceededException.class);

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Plan PREMIUM ne peut pas dépasser 5 objectifs")
        void create_premiumPlanLimit() {
            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("500000"))
                    .monthlyTarget(new BigDecimal("50000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .build();

            when(goalRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(5L);

            assertThatThrownBy(() -> goalService.createGoal(USER_ID, "PREMIUM", request))
                    .isInstanceOf(SavingsGoalLimitExceededException.class);

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Plan PREMIUM_PLUS sans limite")
        void create_premiumPlusNoLimit() {
            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("500000"))
                    .monthlyTarget(new BigDecimal("50000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .build();

            SavingsGoal saved = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            when(goalRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(100L);
            when(goalRepository.save(any(SavingsGoal.class))).thenReturn(saved);
            when(savingsMapper.toGoalDto(saved)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());
            when(recommendationService.computeProgressPercent(saved)).thenReturn(BigDecimal.ZERO);

            SavingsGoalDto dto = goalService.createGoal(USER_ID, "PREMIUM_PLUS", request);

            assertThat(dto.getId()).isEqualTo(GOAL_ID);
        }

        @Test
        @DisplayName("targetDate avant startDate jette InvalidSavingsGoalException")
        void create_invalidDateRange() {
            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("500000"))
                    .startDate(LocalDate.of(2026, 6, 1))
                    .targetDate(LocalDate.of(2026, 1, 1))
                    .build();

            assertThatThrownBy(() -> goalService.createGoal(USER_ID, "PREMIUM", request))
                    .isInstanceOf(InvalidSavingsGoalException.class);

            verify(goalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateGoal")
    class UpdateTests {

        @Test
        @DisplayName("Mise à jour réussie")
        void update_success() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), new BigDecimal("100000"));
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
            when(savingsMapper.toGoalDto(goal)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());

            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage updated")
                    .targetAmount(new BigDecimal("600000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .build();

            SavingsGoalDto dto = goalService.updateGoal(USER_ID, GOAL_ID, request);

            assertThat(dto.getId()).isEqualTo(GOAL_ID);
            assertThat(goal.getName()).isEqualTo("Voyage updated");
            assertThat(goal.getTargetAmount()).isEqualByComparingTo("600000");
        }

        @Test
        @DisplayName("Si currentAmount >= targetAmount, status devient COMPLETED")
        void update_marksCompleted() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), new BigDecimal("500000"));
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
            when(savingsMapper.toGoalDto(goal)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());

            SavingsGoalRequest request = SavingsGoalRequest.builder()
                    .name("Voyage")
                    .targetAmount(new BigDecimal("400000"))
                    .startDate(LocalDate.of(2026, 1, 1))
                    .build();

            goalService.updateGoal(USER_ID, GOAL_ID, request);

            assertThat(goal.getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("getAndVerifyOwnership")
    class OwnershipTests {

        @Test
        @DisplayName("Objectif inexistant - SavingsGoalNotFoundException")
        void goalNotFound() {
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.getGoalById(USER_ID, GOAL_ID, "PREMIUM"))
                    .isInstanceOf(SavingsGoalNotFoundException.class);
        }

        @Test
        @DisplayName("Mauvais propriétaire - UnauthorizedAccessException")
        void wrongOwner() {
            UUID other = UUID.randomUUID();
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            goal.setUserId(other);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> goalService.getGoalById(USER_ID, GOAL_ID, "PREMIUM"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Objectif inactif (soft deleted) - SavingsGoalNotFoundException")
        void inactiveGoal() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            goal.setActive(false);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> goalService.getGoalById(USER_ID, GOAL_ID, "PREMIUM"))
                    .isInstanceOf(SavingsGoalNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGoal (soft)")
    class DeleteTests {

        @Test
        @DisplayName("Soft delete passe active=false et CANCELLED")
        void delete_softDeletes() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            goalService.deleteGoal(USER_ID, GOAL_ID);

            assertThat(goal.isActive()).isFalse();
            assertThat(goal.getStatus()).isEqualTo(SavingsGoalStatus.CANCELLED);
            verify(goalRepository).save(goal);
        }
    }

    @Nested
    @DisplayName("addContribution")
    class ContributionTests {

        @Test
        @DisplayName("Versement met à jour currentAmount et sauvegarde la contribution")
        void contribution_updatesCurrentAmount() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), new BigDecimal("100000"));
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            SavingsContribution saved = SavingsContribution.builder()
                    .id(UUID.randomUUID())
                    .goalId(GOAL_ID)
                    .userId(USER_ID)
                    .amount(new BigDecimal("50000"))
                    .month(5).year(2026)
                    .type(SavingsContributionType.MANUAL)
                    .build();
            when(contributionRepository.save(any(SavingsContribution.class))).thenReturn(saved);
            when(savingsMapper.toContributionDto(saved)).thenReturn(SavingsContributionDto.builder().build());
            when(monthSummaryService.getSummary(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(MonthSummary.builder().status(MonthStatus.NORMAL).build());

            SavingsContributionRequest request = SavingsContributionRequest.builder()
                    .amount(new BigDecimal("50000"))
                    .month(5).year(2026)
                    .type(SavingsContributionType.MANUAL)
                    .build();

            goalService.addContribution(USER_ID, GOAL_ID, request);

            assertThat(goal.getCurrentAmount()).isEqualByComparingTo("150000");
            verify(goalRepository).save(goal);
        }

        @Test
        @DisplayName("Versement qui atteint la cible passe COMPLETED")
        void contribution_marksCompleted() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), new BigDecimal("450000"));
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));
            when(contributionRepository.save(any(SavingsContribution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(savingsMapper.toContributionDto(any())).thenReturn(SavingsContributionDto.builder().build());
            when(monthSummaryService.getSummary(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(MonthSummary.builder().status(MonthStatus.NORMAL).build());

            SavingsContributionRequest request = SavingsContributionRequest.builder()
                    .amount(new BigDecimal("100000"))
                    .month(5).year(2026)
                    .build();

            goalService.addContribution(USER_ID, GOAL_ID, request);

            assertThat(goal.getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);
            assertThat(goal.getCurrentAmount()).isEqualByComparingTo("550000");
        }

        @Test
        @DisplayName("Objectif annulé refuse le versement")
        void contribution_rejectsCancelled() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            goal.setStatus(SavingsGoalStatus.CANCELLED);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            SavingsContributionRequest request = SavingsContributionRequest.builder()
                    .amount(new BigDecimal("50000"))
                    .month(5).year(2026)
                    .build();

            assertThatThrownBy(() -> goalService.addContribution(USER_ID, GOAL_ID, request))
                    .isInstanceOf(InvalidSavingsGoalException.class);
        }
    }

    @Nested
    @DisplayName("getContributions (pagination)")
    class ListContributionsTests {

        @Test
        @DisplayName("Retourne une page de contributions mappées")
        void list_returnsPaginatedDtos() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            SavingsContribution c = SavingsContribution.builder()
                    .id(UUID.randomUUID()).goalId(GOAL_ID).userId(USER_ID)
                    .amount(new BigDecimal("10000")).month(5).year(2026)
                    .type(SavingsContributionType.MANUAL).build();
            Page<SavingsContribution> page = new PageImpl<>(List.of(c));
            when(contributionRepository.findByGoalIdAndUserIdOrderByYearDescMonthDescCreatedAtDesc(
                    eq(GOAL_ID), eq(USER_ID), any(Pageable.class))).thenReturn(page);
            when(savingsMapper.toContributionDto(c)).thenReturn(
                    SavingsContributionDto.builder().month(5).year(2026).build());

            Page<SavingsContributionDto> result = goalService.getContributions(USER_ID, GOAL_ID, "PREMIUM_PLUS", 0, 20);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Plan FREE filtre les contributions au-delà de 3 mois")
        void list_freePlanFiltersOldContributions() {
            SavingsGoal goal = sampleGoal(new BigDecimal("500000"), BigDecimal.ZERO);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));

            SavingsContribution recent = SavingsContribution.builder()
                    .id(UUID.randomUUID()).goalId(GOAL_ID).userId(USER_ID)
                    .amount(new BigDecimal("10000")).month(5).year(2026)
                    .type(SavingsContributionType.MANUAL).build();
            SavingsContribution old = SavingsContribution.builder()
                    .id(UUID.randomUUID()).goalId(GOAL_ID).userId(USER_ID)
                    .amount(new BigDecimal("10000")).month(1).year(2026)
                    .type(SavingsContributionType.MANUAL).build();
            Page<SavingsContribution> page = new PageImpl<>(List.of(recent, old));
            when(contributionRepository.findByGoalIdAndUserIdOrderByYearDescMonthDescCreatedAtDesc(
                    eq(GOAL_ID), eq(USER_ID), any(Pageable.class))).thenReturn(page);
            when(savingsMapper.toContributionDto(recent)).thenReturn(
                    SavingsContributionDto.builder().month(5).year(2026).build());
            when(savingsMapper.toContributionDto(old)).thenReturn(
                    SavingsContributionDto.builder().month(1).year(2026).build());

            Page<SavingsContributionDto> result = goalService.getContributions(USER_ID, GOAL_ID, "FREE", 0, 20);

            // Jan 2026 is more than 3 months before May 2026, so only May remains
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getDashboard")
    class DashboardTests {

        @Test
        @DisplayName("Calcule totalSaved, totalTarget, globalProgress et nextMilestone")
        void dashboard_aggregatesGoals() {
            SavingsGoal a = sampleGoal(new BigDecimal("1000000"), new BigDecimal("300000"));
            SavingsGoal b = sampleGoal(new BigDecimal("500000"), new BigDecimal("250000"));
            when(goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(a, b));
            when(recommendationService.computeProgressPercent(any())).thenReturn(new BigDecimal("50.00"));
            when(monthSummaryService.getSummary(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(MonthSummary.builder().status(MonthStatus.NORMAL).build());
            when(recommendationService.calculateRecommendations(eq(USER_ID), any(MonthSummary.class)))
                    .thenReturn(List.of());

            SavingsDashboardDto dashboard = goalService.getDashboard(USER_ID);

            assertThat(dashboard.getTotalSaved()).isEqualByComparingTo("550000");
            assertThat(dashboard.getTotalTarget()).isEqualByComparingTo("1500000");
            // 550000 * 100 / 1500000 ≈ 36.67
            assertThat(dashboard.getGlobalProgressPercent()).isEqualByComparingTo("36.67");
            assertThat(dashboard.getActiveGoalsCount()).isEqualTo(2);
            // Goal b has smallest remaining (250000 vs 700000)
            assertThat(dashboard.getNextMilestone()).isNotNull();
            assertThat(dashboard.getNextMilestone().getRemainingAmount()).isEqualByComparingTo("250000");
        }

        @Test
        @DisplayName("Dashboard sans objectifs retourne zéros")
        void dashboard_emptyGoals() {
            when(goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of());
            // Pas de getMonthlyRecommendation appelé car catch englobe; on stub par sécurité.
            when(monthSummaryService.getSummary(eq(USER_ID), anyInt(), anyInt()))
                    .thenReturn(MonthSummary.builder().status(MonthStatus.NORMAL).build());
            when(recommendationService.calculateRecommendations(eq(USER_ID), any(MonthSummary.class)))
                    .thenReturn(List.of());

            SavingsDashboardDto dashboard = goalService.getDashboard(USER_ID);

            assertThat(dashboard.getTotalSaved()).isEqualByComparingTo("0");
            assertThat(dashboard.getTotalTarget()).isEqualByComparingTo("0");
            assertThat(dashboard.getGlobalProgressPercent()).isEqualByComparingTo("0");
            assertThat(dashboard.getActiveGoalsCount()).isZero();
            assertThat(dashboard.getNextMilestone()).isNull();
        }
    }

    @Nested
    @DisplayName("getMonthlyRecommendation")
    class MonthlyRecoTests {

        @Test
        @DisplayName("Délègue à MonthSummaryService puis SavingsRecommendationService")
        void monthly_delegates() {
            MonthSummary summary = MonthSummary.builder()
                    .userId(USER_ID).month(5).year(2026).status(MonthStatus.NORMAL).build();
            when(monthSummaryService.getSummary(USER_ID, 5, 2026)).thenReturn(summary);
            when(recommendationService.calculateRecommendations(USER_ID, summary))
                    .thenReturn(List.of(SavingsRecommendationDto.builder().build()));

            List<SavingsRecommendationDto> result = goalService.getMonthlyRecommendation(USER_ID, 5, 2026);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getGoals")
    class ListGoalsTests {

        @Test
        @DisplayName("Liste tous les objectifs actifs enrichis avec exportAllowed")
        void list_returnsEnrichedDtos() {
            SavingsGoal g = sampleGoal(new BigDecimal("500000"), new BigDecimal("100000"));
            when(goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(g));
            when(savingsMapper.toGoalDto(g)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());
            when(recommendationService.computeProgressPercent(g)).thenReturn(new BigDecimal("20.00"));

            List<SavingsGoalDto> result = goalService.getGoals(USER_ID, "PREMIUM");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProgressPercent()).isEqualByComparingTo("20.00");
            assertThat(result.get(0).isExportAllowed()).isTrue();
        }

        @Test
        @DisplayName("Plan FREE ne permet pas l'export")
        void list_freePlanNoExport() {
            SavingsGoal g = sampleGoal(new BigDecimal("500000"), new BigDecimal("100000"));
            when(goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(g));
            when(savingsMapper.toGoalDto(g)).thenReturn(SavingsGoalDto.builder().id(GOAL_ID).build());
            when(recommendationService.computeProgressPercent(g)).thenReturn(new BigDecimal("20.00"));

            List<SavingsGoalDto> result = goalService.getGoals(USER_ID, "FREE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isExportAllowed()).isFalse();
        }
    }
}
