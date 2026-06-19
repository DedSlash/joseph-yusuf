package com.josephyusuf.income.tips.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.service.MonthSummaryService;
import com.josephyusuf.income.tips.catalog.MoneyTip;
import com.josephyusuf.income.tips.catalog.MoneyTipsCatalog;
import com.josephyusuf.income.tips.dto.MoneyTipDto;
import com.josephyusuf.income.tips.dto.MoneyTipsDto;
import com.josephyusuf.income.tips.dto.RecommendedSplitDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoneyTipsService {

    private static final BigDecimal NEEDS_RATE = new BigDecimal("0.50");
    private static final BigDecimal WANTS_RATE = new BigDecimal("0.30");
    private static final BigDecimal SAVINGS_RATE = new BigDecimal("0.20");

    private final MonthSummaryService summaryService;

    public MoneyTipsDto getTips(UUID userId, int month, int year, String plan,
                                 String country, String currency, Locale locale) {
        MonthSummary summary = summaryService.getSummary(userId, month, year);

        String safePlan = plan != null ? plan : MoneyTipsCatalog.PLAN_FREE;
        String safeCountry = country != null && !country.isBlank() ? country.toUpperCase() : "SN";
        String safeCurrency = currency != null && !currency.isBlank() ? currency.toUpperCase() : "XOF";
        boolean french = locale == null || "fr".equalsIgnoreCase(locale.getLanguage());

        BigDecimal totalIncome = summary.getTotalIncome() != null ? summary.getTotalIncome() : BigDecimal.ZERO;
        BigDecimal recommendedSavings = computeRecommendedSavings(totalIncome,
                summary.getAverageLast3Months(), summary.getStatus());
        RecommendedSplitDto split = computeSplit(totalIncome);

        List<MoneyTipDto> tips = filterAndLocalize(safeCountry, safePlan, summary.getStatus(), french);

        return MoneyTipsDto.builder()
                .josephStatus(summary.getStatus())
                .totalAmount(totalIncome)
                .currency(safeCurrency)
                .country(safeCountry)
                .recommendedSavings(recommendedSavings)
                .recommendedSplit(split)
                .tips(tips)
                .build();
    }

    private BigDecimal computeRecommendedSavings(BigDecimal totalIncome, BigDecimal average3,
                                                  MonthStatus status) {
        if (totalIncome == null || totalIncome.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (status == MonthStatus.LEAN) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (status == MonthStatus.ABUNDANCE && average3 != null && average3.signum() > 0) {
            BigDecimal excess = totalIncome.subtract(average3).max(BigDecimal.ZERO);
            BigDecimal base = totalIncome.multiply(SAVINGS_RATE);
            return excess.add(base).setScale(2, RoundingMode.HALF_UP);
        }
        return totalIncome.multiply(SAVINGS_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private RecommendedSplitDto computeSplit(BigDecimal totalIncome) {
        BigDecimal income = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        return RecommendedSplitDto.builder()
                .needs(income.multiply(NEEDS_RATE).setScale(2, RoundingMode.HALF_UP))
                .wants(income.multiply(WANTS_RATE).setScale(2, RoundingMode.HALF_UP))
                .savings(income.multiply(SAVINGS_RATE).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private List<MoneyTipDto> filterAndLocalize(String country, String plan, MonthStatus status,
                                                 boolean french) {
        List<MoneyTipDto> result = new ArrayList<>();

        for (MoneyTip tip : MoneyTipsCatalog.TIPS) {
            if (!matchesCountry(tip, country) || (status == MonthStatus.LEAN && tip.isHiddenInLean())) {
                continue;
            }
            boolean locked = isLocked(plan, tip.getRequiredPlan());
            result.add(toDto(tip, locked, french));
        }

        sortByPriority(result, status);
        return result;
    }

    private boolean matchesCountry(MoneyTip tip, String country) {
        return tip.getCountries() == null
                || tip.getCountries().isEmpty()
                || tip.getCountries().contains(country);
    }

    private boolean isLocked(String userPlan, String requiredPlan) {
        return planRank(userPlan) < planRank(requiredPlan);
    }

    private int planRank(String plan) {
        if (MoneyTipsCatalog.PLAN_PREMIUM_PLUS.equalsIgnoreCase(plan)) return 2;
        if (MoneyTipsCatalog.PLAN_PREMIUM.equalsIgnoreCase(plan)) return 1;
        return 0;
    }

    private MoneyTipDto toDto(MoneyTip tip, boolean locked, boolean french) {
        String description = french ? tip.getDescriptionFr() : tip.getDescriptionEn();
        return MoneyTipDto.builder()
                .id(tip.getId())
                .title(french ? tip.getTitleFr() : tip.getTitleEn())
                .description(description)
                .icon(tip.getIcon())
                .method(tip.getMethod() != null ? tip.getMethod().name() : null)
                .countries(tip.getCountries())
                .requiredPlan(tip.getRequiredPlan())
                .locked(locked)
                .actionUrl(tip.getActionUrl())
                .actionLabel(french ? tip.getActionLabelFr() : tip.getActionLabelEn())
                .build();
    }

    private void sortByPriority(List<MoneyTipDto> tips, MonthStatus status) {
        if (status == MonthStatus.ABUNDANCE) {
            tips.sort(Comparator.comparingInt(t -> isAdvancedTip(t.getId()) ? 0 : 1));
            return;
        }
        if (status == MonthStatus.LEAN) {
            tips.sort(Comparator.comparingInt(t -> "TIP_001".equals(t.getId()) ? 0 : 1));
        }
    }

    private boolean isAdvancedTip(String id) {
        return "TIP_007".equals(id) || "TIP_008".equals(id) || "TIP_009".equals(id);
    }
}
