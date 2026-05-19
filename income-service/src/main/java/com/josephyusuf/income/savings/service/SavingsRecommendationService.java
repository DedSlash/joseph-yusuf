package com.josephyusuf.income.savings.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.savings.dto.SavingsRecommendationDto;
import com.josephyusuf.income.savings.entity.SavingsGoal;
import com.josephyusuf.income.savings.entity.SavingsGoalStatus;
import com.josephyusuf.income.savings.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @ExtractToSavingsService — calcule la recommandation d'épargne mensuelle
 * selon le Principe de Joseph et répartit le total entre objectifs actifs
 * au prorata du restant à atteindre.
 */
@Service
@RequiredArgsConstructor
public class SavingsRecommendationService {

    // % du revenu en mois NORMAL (fallback si pas de monthlyTarget configuré)
    private static final BigDecimal NORMAL_PERCENT_OF_INCOME = new BigDecimal("0.15");
    // Coefficient pour conserver une part de la base en mois d'ABUNDANCE
    private static final BigDecimal ABUNDANCE_BASE_RETENTION = new BigDecimal("0.5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SavingsGoalRepository goalRepository;

    public List<SavingsRecommendationDto> calculateRecommendations(UUID userId, MonthSummary summary) {
        List<SavingsGoal> activeGoals = goalRepository.findByUserIdAndActiveTrueAndStatus(
                userId, SavingsGoalStatus.ACTIVE);
        if (activeGoals.isEmpty()) {
            return List.of();
        }

        BigDecimal totalReco = computeGlobalRecommendation(summary, activeGoals);
        BigDecimal totalRemaining = sumRemaining(activeGoals);

        List<SavingsRecommendationDto> recommendations = new ArrayList<>();
        for (SavingsGoal goal : activeGoals) {
            BigDecimal share = computeShare(goal, totalReco, totalRemaining);
            recommendations.add(buildRecommendation(goal, share, summary));
        }
        return recommendations;
    }

    public SavingsRecommendationDto buildRecommendation(SavingsGoal goal,
                                                        BigDecimal recommendedAmount,
                                                        MonthSummary summary) {
        BigDecimal progress = computeProgressPercent(goal);
        LocalDate projected = projectCompletionDate(goal, recommendedAmount);

        return SavingsRecommendationDto.builder()
                .goalId(goal.getId())
                .goalName(goal.getName())
                .recommendedAmount(recommendedAmount.setScale(2, RoundingMode.HALF_UP))
                .josephStatus(summary.getStatus())
                .message(buildMessage(summary.getStatus(), recommendedAmount, goal.getName()))
                .progressPercent(progress)
                .projectedCompletionDate(projected)
                .month(summary.getMonth())
                .year(summary.getYear())
                .build();
    }

    public BigDecimal computeProgressPercent(SavingsGoal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        BigDecimal pct = current.multiply(HUNDRED)
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        if (pct.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return pct;
    }

    public LocalDate projectCompletionDate(SavingsGoal goal, BigDecimal monthlyContribution) {
        if (monthlyContribution == null || monthlyContribution.signum() <= 0) {
            return null;
        }
        BigDecimal remaining = remainingFor(goal);
        if (remaining.signum() <= 0) {
            return LocalDate.now();
        }
        int monthsNeeded = remaining.divide(monthlyContribution, 0, RoundingMode.CEILING).intValue();
        if (monthsNeeded <= 0) {
            return LocalDate.now();
        }
        return LocalDate.now().plusMonths(monthsNeeded);
    }

    private BigDecimal computeGlobalRecommendation(MonthSummary summary, List<SavingsGoal> goals) {
        MonthStatus status = summary.getStatus();
        BigDecimal totalIncome = summary.getTotalIncome() != null ? summary.getTotalIncome() : BigDecimal.ZERO;
        BigDecimal averageLast3 = summary.getAverageLast3Months() != null ? summary.getAverageLast3Months() : BigDecimal.ZERO;
        BigDecimal baseTarget = aggregateBaseTarget(goals, totalIncome);

        return switch (status) {
            case ABUNDANCE -> {
                BigDecimal surplus = totalIncome.subtract(averageLast3).max(BigDecimal.ZERO);
                BigDecimal abundanceReco = surplus.add(baseTarget.multiply(ABUNDANCE_BASE_RETENTION));
                yield baseTarget.max(abundanceReco);
            }
            case LEAN -> BigDecimal.ZERO;
            case NORMAL -> baseTarget;
        };
    }

    /**
     * Agrège pour chaque objectif MAX(monthlyTarget, monthlyTargetPercent * revenu),
     * puis somme — ça donne la base totale à allouer en mois NORMAL.
     */
    private BigDecimal aggregateBaseTarget(List<SavingsGoal> goals, BigDecimal totalIncome) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SavingsGoal goal : goals) {
            sum = sum.add(perGoalBaseTarget(goal, totalIncome));
        }
        return sum;
    }

    private BigDecimal perGoalBaseTarget(SavingsGoal goal, BigDecimal totalIncome) {
        BigDecimal monthlyTarget = goal.getMonthlyTarget() == null ? BigDecimal.ZERO : goal.getMonthlyTarget();
        BigDecimal pct = goal.getMonthlyTargetPercent();
        BigDecimal pctAmount = BigDecimal.ZERO;
        if (pct != null && pct.signum() > 0 && totalIncome.signum() > 0) {
            pctAmount = pct.multiply(totalIncome)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        BigDecimal base = monthlyTarget.max(pctAmount);
        if (base.signum() == 0) {
            // Fallback : si ni monthlyTarget ni % renseignés, on utilise NORMAL_PERCENT_OF_INCOME
            // réparti uniformément (sera affiné par la suite via le prorata).
            base = totalIncome.multiply(NORMAL_PERCENT_OF_INCOME)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return base;
    }

    private BigDecimal computeShare(SavingsGoal goal, BigDecimal totalReco, BigDecimal totalRemaining) {
        if (totalReco.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal remaining = remainingFor(goal);
        if (totalRemaining.signum() <= 0 || remaining.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return totalReco.multiply(remaining)
                .divide(totalRemaining, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumRemaining(List<SavingsGoal> goals) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SavingsGoal g : goals) {
            sum = sum.add(remainingFor(g));
        }
        return sum;
    }

    private BigDecimal remainingFor(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        return target.subtract(current).max(BigDecimal.ZERO);
    }

    private String buildMessage(MonthStatus status, BigDecimal amount, String goalName) {
        return switch (status) {
            case ABUNDANCE -> String.format(
                    "Mois d'abondance — on profite du surplus pour pousser fort sur \"%s\". Reco : %s.",
                    goalName, formatAmount(amount));
            case LEAN -> String.format(
                    "Mois de disette — on met \"%s\" en pause et on puise dans l'épargne d'abondance si besoin.",
                    goalName);
            case NORMAL -> String.format(
                    "Mois normal — versement régulier sur \"%s\". Reco : %s.",
                    goalName, formatAmount(amount));
        };
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0 XOF";
        return String.format("%,.0f XOF", amount).replace(",", " ");
    }
}
