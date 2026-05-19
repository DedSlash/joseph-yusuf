package com.josephyusuf.income.savings.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.savings.dto.SavingsRecommendationDto;
import com.josephyusuf.income.savings.entity.SavingsGoal;
import com.josephyusuf.income.savings.entity.SavingsGoalStatus;
import com.josephyusuf.income.savings.repository.SavingsGoalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsRecommendationServiceTest {

    @Mock
    private SavingsGoalRepository goalRepository;

    @InjectMocks
    private SavingsRecommendationService recommendationService;

    private static final UUID USER_ID = UUID.randomUUID();

    private SavingsGoal goal(BigDecimal target, BigDecimal current, BigDecimal monthlyTarget) {
        return SavingsGoal.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .name("Fonds d'urgence")
                .targetAmount(target)
                .currentAmount(current)
                .monthlyTarget(monthlyTarget)
                .startDate(LocalDate.now().minusMonths(1))
                .status(SavingsGoalStatus.ACTIVE)
                .active(true)
                .build();
    }

    private MonthSummary summary(MonthStatus status, BigDecimal totalIncome, BigDecimal avg) {
        return MonthSummary.builder()
                .userId(USER_ID)
                .month(5)
                .year(2026)
                .totalIncome(totalIncome)
                .averageLast3Months(avg)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("calculateRecommendations - cas vides")
    class EmptyCases {

        @Test
        @DisplayName("Aucun objectif actif retourne une liste vide")
        void noActiveGoals_returnsEmpty() {
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of());

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000")));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateRecommendations - statut LEAN")
    class LeanCase {

        @Test
        @DisplayName("Mois de disette recommande 0 pour tous les objectifs")
        void lean_recommendsZero() {
            SavingsGoal g = goal(new BigDecimal("1000000"), new BigDecimal("200000"), new BigDecimal("50000"));
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(g));

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.LEAN, new BigDecimal("300000"), new BigDecimal("500000")));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.get(0).getJosephStatus()).isEqualTo(MonthStatus.LEAN);
            assertThat(result.get(0).getMessage()).contains("disette");
        }
    }

    @Nested
    @DisplayName("calculateRecommendations - statut NORMAL")
    class NormalCase {

        @Test
        @DisplayName("NORMAL avec un seul objectif et monthlyTarget retourne monthlyTarget")
        void normal_singleGoal_returnsMonthlyTarget() {
            SavingsGoal g = goal(new BigDecimal("1000000"), new BigDecimal("200000"), new BigDecimal("50000"));
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(g));

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000")));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo("50000.00");
            assertThat(result.get(0).getJosephStatus()).isEqualTo(MonthStatus.NORMAL);
        }

        @Test
        @DisplayName("NORMAL avec monthlyTargetPercent prend le MAX(target, percent*income)")
        void normal_percent_takesMaxOfBoth() {
            SavingsGoal g = SavingsGoal.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .name("Voyage")
                    .targetAmount(new BigDecimal("2000000"))
                    .currentAmount(BigDecimal.ZERO)
                    .monthlyTarget(new BigDecimal("30000"))
                    .monthlyTargetPercent(new BigDecimal("15"))
                    .startDate(LocalDate.now())
                    .status(SavingsGoalStatus.ACTIVE)
                    .active(true)
                    .build();
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(g));

            // 15% * 500000 = 75000 > 30000 -> retient 75000
            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000")));

            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo("75000.00");
        }
    }

    @Nested
    @DisplayName("calculateRecommendations - statut ABUNDANCE")
    class AbundanceCase {

        @Test
        @DisplayName("ABUNDANCE recommande surplus + base*0.5 (ou base si plus grand)")
        void abundance_includesSurplusPlusHalfBase() {
            // revenu=700000, moyenne=500000, surplus=200000, monthlyTarget=50000 -> surplus + 25000 = 225000
            SavingsGoal g = goal(new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("50000"));
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(g));

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.ABUNDANCE, new BigDecimal("700000"), new BigDecimal("500000")));

            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo("225000.00");
            assertThat(result.get(0).getJosephStatus()).isEqualTo(MonthStatus.ABUNDANCE);
        }

        @Test
        @DisplayName("ABUNDANCE retient au minimum la base si surplus faible")
        void abundance_neverBelowBase() {
            // surplus=10000, base=50000 -> doit retourner au moins base
            SavingsGoal g = goal(new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("50000"));
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(g));

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.ABUNDANCE, new BigDecimal("510000"), new BigDecimal("500000")));

            // base = 50000, abundanceReco = 10000 + 25000 = 35000 -> max(50000, 35000) = 50000
            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo("50000.00");
        }
    }

    @Nested
    @DisplayName("calculateRecommendations - répartition prorata multi-objectifs")
    class ProrataCase {

        @Test
        @DisplayName("Répartition au prorata du restant à atteindre")
        void multipleGoals_distributesProRata() {
            // Goal A: remaining = 800000, Goal B: remaining = 200000 -> total remaining = 1000000
            // Total reco (NORMAL) = 50000 + 30000 = 80000
            // A reçoit 80000 * 800000/1000000 = 64000
            // B reçoit 80000 * 200000/1000000 = 16000
            SavingsGoal a = goal(new BigDecimal("1000000"), new BigDecimal("200000"), new BigDecimal("50000"));
            SavingsGoal b = goal(new BigDecimal("500000"), new BigDecimal("300000"), new BigDecimal("30000"));
            when(goalRepository.findByUserIdAndActiveTrueAndStatus(USER_ID, SavingsGoalStatus.ACTIVE))
                    .thenReturn(List.of(a, b));

            List<SavingsRecommendationDto> result = recommendationService.calculateRecommendations(
                    USER_ID, summary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000")));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRecommendedAmount()).isEqualByComparingTo("64000.00");
            assertThat(result.get(1).getRecommendedAmount()).isEqualByComparingTo("16000.00");
        }
    }

    @Nested
    @DisplayName("computeProgressPercent")
    class ProgressTests {

        @Test
        @DisplayName("Progress = current * 100 / target")
        void progress_normalCase() {
            SavingsGoal g = goal(new BigDecimal("1000"), new BigDecimal("250"), null);
            assertThat(recommendationService.computeProgressPercent(g)).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("Progress capé à 100 si current dépasse target")
        void progress_capped_at_100() {
            SavingsGoal g = goal(new BigDecimal("1000"), new BigDecimal("2000"), null);
            assertThat(recommendationService.computeProgressPercent(g)).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("Target=0 retourne 0")
        void progress_zeroTarget() {
            SavingsGoal g = SavingsGoal.builder().targetAmount(BigDecimal.ZERO).currentAmount(BigDecimal.ZERO).build();
            assertThat(recommendationService.computeProgressPercent(g)).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("projectCompletionDate")
    class ProjectionTests {

        @Test
        @DisplayName("Projection basée sur le restant / contribution mensuelle")
        void projection_calculatesCorrectly() {
            SavingsGoal g = goal(new BigDecimal("1000"), new BigDecimal("100"), null);
            LocalDate projected = recommendationService.projectCompletionDate(g, new BigDecimal("100"));
            // remaining = 900, contribution = 100 -> 9 mois
            assertThat(projected).isEqualTo(LocalDate.now().plusMonths(9));
        }

        @Test
        @DisplayName("Contribution nulle retourne null")
        void projection_nullForZeroContribution() {
            SavingsGoal g = goal(new BigDecimal("1000"), BigDecimal.ZERO, null);
            assertThat(recommendationService.projectCompletionDate(g, BigDecimal.ZERO)).isNull();
            assertThat(recommendationService.projectCompletionDate(g, null)).isNull();
        }

        @Test
        @DisplayName("Objectif déjà atteint retourne la date du jour")
        void projection_alreadyReached() {
            SavingsGoal g = goal(new BigDecimal("1000"), new BigDecimal("1000"), null);
            assertThat(recommendationService.projectCompletionDate(g, new BigDecimal("100")))
                    .isEqualTo(LocalDate.now());
        }
    }
}
