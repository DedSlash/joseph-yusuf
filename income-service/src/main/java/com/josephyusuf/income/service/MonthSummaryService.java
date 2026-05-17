package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonthSummaryService {

    private static final BigDecimal ABUNDANCE_FACTOR = new BigDecimal("1.15");
    private static final BigDecimal LEAN_FACTOR = new BigDecimal("0.85");

    private final IncomeEntryRepository entryRepository;

    public MonthSummary getSummary(UUID userId, int month, int year) {
        BigDecimal totalIncome = entryRepository.sumByUserIdAndMonthAndYear(userId, month, year);
        int[] foundMonths = {0};
        BigDecimal averageLast3 = calculateAveragePreviousMonths(userId, month, year, 3, foundMonths);

        BigDecimal abundanceThreshold = averageLast3.multiply(ABUNDANCE_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal leanThreshold = averageLast3.multiply(LEAN_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);

        MonthStatus status = determineStatus(totalIncome, averageLast3);

        double percentageVsAverage = 0.0;
        if (averageLast3.compareTo(BigDecimal.ZERO) > 0) {
            percentageVsAverage = totalIncome.subtract(averageLast3)
                    .divide(averageLast3, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }

        return MonthSummary.builder()
                .userId(userId)
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .averageLast3Months(averageLast3)
                .abundanceThreshold(abundanceThreshold)
                .leanThreshold(leanThreshold)
                .status(status)
                .percentageVsAverage(percentageVsAverage)
                .monthsInBaseline(foundMonths[0])
                .build();
    }

    public List<MonthSummary> getHistory(UUID userId, int months) {
        // Récupère les N derniers mois ayant réellement des entrées (pas forcément consécutifs)
        List<String> distinctMonths = entryRepository.findDistinctMonthsByUserId(userId);

        List<MonthSummary> history = new ArrayList<>();
        int limit = Math.min(months, distinctMonths.size());

        for (int i = 0; i < limit; i++) {
            String[] parts = distinctMonths.get(i).split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            history.add(getSummary(userId, m, y));
        }

        return history;
    }

    private BigDecimal calculateAveragePreviousMonths(UUID userId, int month, int year, int count, int[] foundOut) {
        BigDecimal sum = BigDecimal.ZERO;
        int found = 0;

        for (int i = 1; i <= count; i++) {
            int m = month - i;
            int y = year;
            while (m <= 0) {
                m += 12;
                y--;
            }

            BigDecimal monthTotal = entryRepository.sumByUserIdAndMonthAndYear(userId, m, y);
            if (monthTotal.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(monthTotal);
                found++;
            }
        }

        if (foundOut != null) foundOut[0] = found;

        if (found == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(new BigDecimal(found), 2, RoundingMode.HALF_UP);
    }

    private MonthStatus determineStatus(BigDecimal totalIncome, BigDecimal average) {
        if (average.compareTo(BigDecimal.ZERO) == 0) {
            return MonthStatus.NORMAL;
        }

        BigDecimal abundanceThreshold = average.multiply(ABUNDANCE_FACTOR);
        BigDecimal leanThreshold = average.multiply(LEAN_FACTOR);

        if (totalIncome.compareTo(abundanceThreshold) > 0) {
            return MonthStatus.ABUNDANCE;
        } else if (totalIncome.compareTo(leanThreshold) < 0) {
            return MonthStatus.LEAN;
        }

        return MonthStatus.NORMAL;
    }
}
