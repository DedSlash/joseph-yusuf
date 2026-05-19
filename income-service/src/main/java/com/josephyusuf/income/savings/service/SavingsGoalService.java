package com.josephyusuf.income.savings.service;

import com.josephyusuf.income.dto.MonthSummary;
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
import com.josephyusuf.income.savings.util.PlanRestrictions;
import com.josephyusuf.income.service.MonthSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @ExtractToSavingsService — vivra dans savings-service après extraction.
 */
@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SavingsGoalRepository goalRepository;
    private final SavingsContributionRepository contributionRepository;
    private final SavingsMapper savingsMapper;
    private final SavingsRecommendationService recommendationService;
    private final MonthSummaryService monthSummaryService;

    @Transactional
    public SavingsGoalDto createGoal(UUID userId, String plan, SavingsGoalRequest request) {
        validateRequest(request);

        long activeCount = goalRepository.countByUserIdAndActiveTrue(userId);
        int maxGoals = switch (plan) {
            case "PREMIUM_PLUS" -> Integer.MAX_VALUE;
            case "PREMIUM" -> PlanRestrictions.PREMIUM_MAX_SAVINGS_GOALS;
            default -> PlanRestrictions.FREE_MAX_SAVINGS_GOALS;
        };
        if (activeCount >= maxGoals) {
            throw new SavingsGoalLimitExceededException(
                "Limite d'objectifs d'épargne atteinte pour votre plan (" + maxGoals + " max). Passez au plan supérieur pour en créer davantage."
            );
        }

        SavingsGoal goal = SavingsGoal.builder()
                .userId(userId)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .monthlyTarget(request.getMonthlyTarget())
                .monthlyTargetPercent(request.getMonthlyTargetPercent())
                .startDate(request.getStartDate())
                .targetDate(request.getTargetDate())
                .status(SavingsGoalStatus.ACTIVE)
                .active(true)
                .build();

        goal = goalRepository.save(goal);
        return enrich(goal);
    }

    @Transactional
    public SavingsGoalDto updateGoal(UUID userId, UUID goalId, SavingsGoalRequest request) {
        validateRequest(request);
        SavingsGoal goal = getAndVerifyOwnership(userId, goalId);

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setMonthlyTarget(request.getMonthlyTarget());
        goal.setMonthlyTargetPercent(request.getMonthlyTargetPercent());
        goal.setStartDate(request.getStartDate());
        goal.setTargetDate(request.getTargetDate());

        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoalStatus.COMPLETED);
        }

        goal = goalRepository.save(goal);
        return enrich(goal);
    }

    public List<SavingsGoalDto> getGoals(UUID userId, String plan) {
        return goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId).stream()
                .map(g -> enrichWithPlan(g, plan))
                .toList();
    }

    public SavingsGoalDto getGoalById(UUID userId, UUID goalId, String plan) {
        SavingsGoal goal = getAndVerifyOwnership(userId, goalId);
        return enrichWithPlan(goal, plan);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        SavingsGoal goal = getAndVerifyOwnership(userId, goalId);
        goal.setActive(false);
        goal.setStatus(SavingsGoalStatus.CANCELLED);
        goalRepository.save(goal);
    }

    @Transactional
    public SavingsContributionDto addContribution(UUID userId, UUID goalId, SavingsContributionRequest request) {
        SavingsGoal goal = getAndVerifyOwnership(userId, goalId);
        if (goal.getStatus() == SavingsGoalStatus.CANCELLED) {
            throw new InvalidSavingsGoalException("Impossible d'ajouter un versement à un objectif annulé");
        }

        LocalDate today = LocalDate.now();
        int month = request.getMonth() != null ? request.getMonth() : today.getMonthValue();
        int year = request.getYear() != null ? request.getYear() : today.getYear();
        SavingsContributionType type = request.getType() != null ? request.getType() : SavingsContributionType.MANUAL;

        com.josephyusuf.income.entity.MonthStatus josephStatus = null;
        try {
            MonthSummary summary = monthSummaryService.getSummary(userId, month, year);
            josephStatus = summary.getStatus();
        } catch (Exception ignored) {
            // Pas de summary disponible — on enregistre le versement sans statut.
        }

        SavingsContribution contribution = SavingsContribution.builder()
                .goalId(goal.getId())
                .userId(userId)
                .amount(request.getAmount())
                .month(month)
                .year(year)
                .type(type)
                .josephStatus(josephStatus)
                .note(request.getNote())
                .build();
        contribution = contributionRepository.save(contribution);

        goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoalStatus.COMPLETED);
        }
        goalRepository.save(goal);

        return savingsMapper.toContributionDto(contribution);
    }

    public Page<SavingsContributionDto> getContributions(UUID userId, UUID goalId, String plan, int page, int size) {
        getAndVerifyOwnership(userId, goalId);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavingsContributionDto> result = contributionRepository
                .findByGoalIdAndUserIdOrderByYearDescMonthDescCreatedAtDesc(goalId, userId, pageable)
                .map(savingsMapper::toContributionDto);

        // For FREE/PREMIUM, filter contributions to last N months
        if (!"PREMIUM_PLUS".equals(plan)) {
            int maxMonths = "PREMIUM".equals(plan)
                ? PlanRestrictions.PREMIUM_SAVINGS_HISTORY_MONTHS
                : PlanRestrictions.FREE_SAVINGS_HISTORY_MONTHS;
            LocalDate cutoff = LocalDate.now().minusMonths(maxMonths);
            int cutoffYear = cutoff.getYear();
            int cutoffMonth = cutoff.getMonthValue();

            List<SavingsContributionDto> filtered = result.getContent().stream()
                    .filter(c -> c.getYear() > cutoffYear || (c.getYear() == cutoffYear && c.getMonth() >= cutoffMonth))
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }
        return result;
    }

    public List<SavingsRecommendationDto> getMonthlyRecommendation(UUID userId, int month, int year) {
        MonthSummary summary = monthSummaryService.getSummary(userId, month, year);
        return recommendationService.calculateRecommendations(userId, summary);
    }

    public SavingsDashboardDto getDashboard(UUID userId) {
        List<SavingsGoal> goals = goalRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);

        BigDecimal totalSaved = goals.stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTarget = goals.stream()
                .map(SavingsGoal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal globalProgress = totalTarget.signum() > 0
                ? totalSaved.multiply(HUNDRED).divide(totalTarget, 2, RoundingMode.HALF_UP).min(HUNDRED)
                : BigDecimal.ZERO;

        long activeGoalsCount = goals.stream()
                .filter(g -> g.getStatus() == SavingsGoalStatus.ACTIVE)
                .count();

        LocalDate now = LocalDate.now();
        List<SavingsRecommendationDto> recommendations;
        try {
            recommendations = getMonthlyRecommendation(userId, now.getMonthValue(), now.getYear());
        } catch (Exception e) {
            recommendations = List.of();
        }

        SavingsDashboardDto.NextMilestoneDto nextMilestone = goals.stream()
                .filter(g -> g.getStatus() == SavingsGoalStatus.ACTIVE)
                .min(Comparator.comparing(g -> g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO)))
                .map(g -> SavingsDashboardDto.NextMilestoneDto.builder()
                        .goalId(g.getId())
                        .goalName(g.getName())
                        .remainingAmount(g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO))
                        .progressPercent(recommendationService.computeProgressPercent(g))
                        .build())
                .orElse(null);

        return SavingsDashboardDto.builder()
                .totalSaved(totalSaved)
                .totalTarget(totalTarget)
                .globalProgressPercent(globalProgress)
                .activeGoalsCount(activeGoalsCount)
                .monthlyRecommendations(recommendations)
                .nextMilestone(nextMilestone)
                .build();
    }

    public SavingsGoal getAndVerifyOwnership(UUID userId, UUID goalId) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new SavingsGoalNotFoundException("Objectif d'épargne introuvable"));
        if (!goal.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès non autorisé à cet objectif d'épargne");
        }
        if (!goal.isActive()) {
            throw new SavingsGoalNotFoundException("Objectif d'épargne introuvable");
        }
        return goal;
    }

    private void validateRequest(SavingsGoalRequest request) {
        if (request.getTargetDate() != null && request.getTargetDate().isBefore(request.getStartDate())) {
            throw new InvalidSavingsGoalException("La date cible doit être postérieure à la date de début");
        }
    }

    private SavingsGoalDto enrich(SavingsGoal goal) {
        SavingsGoalDto dto = savingsMapper.toGoalDto(goal);
        dto.setProgressPercent(recommendationService.computeProgressPercent(goal));
        BigDecimal monthlyContribution = goal.getMonthlyTarget() != null && goal.getMonthlyTarget().signum() > 0
                ? goal.getMonthlyTarget()
                : null;
        if (monthlyContribution != null) {
            dto.setProjectedCompletionDate(recommendationService.projectCompletionDate(goal, monthlyContribution));
        }
        return dto;
    }

    private SavingsGoalDto enrichWithPlan(SavingsGoal goal, String plan) {
        SavingsGoalDto dto = enrich(goal);
        dto.setExportAllowed(!"FREE".equals(plan));
        return dto;
    }
}
