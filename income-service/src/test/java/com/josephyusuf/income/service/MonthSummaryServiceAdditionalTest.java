package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthSummaryServiceAdditionalTest {

    @Mock
    private IncomeEntryRepository entryRepository;

    @InjectMocks
    private MonthSummaryService summaryService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("getHistory - returns empty when user has no entries")
    void getHistory_noEntries_returnsEmpty() {
        when(entryRepository.findDistinctMonthsByUserId(USER_ID)).thenReturn(List.of());

        List<MonthSummary> result = summaryService.getHistory(USER_ID, 3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getHistory - returns N most recent months with entries")
    void getHistory_includesMonthsWithEntries() {
        when(entryRepository.findDistinctMonthsByUserId(USER_ID))
                .thenReturn(List.of("2026-05", "2026-04"));
        when(entryRepository.sumByUserIdAndMonthAndYear(any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("500000"));

        List<MonthSummary> result = summaryService.getHistory(USER_ID, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getSummary - average zero returns NORMAL status")
    void getSummary_zeroAverage_returnsNormal() {
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2026))
                .thenReturn(new BigDecimal("500000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 12, 2025))
                .thenReturn(BigDecimal.ZERO);
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 11, 2025))
                .thenReturn(BigDecimal.ZERO);
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 10, 2025))
                .thenReturn(BigDecimal.ZERO);

        MonthSummary result = summaryService.getSummary(USER_ID, 1, 2026);

        assertThat(result.getStatus()).isEqualTo(MonthStatus.NORMAL);
        assertThat(result.getPercentageVsAverage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getSummary - ABUNDANCE when income > average * 1.15")
    void getSummary_abundance() {
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 5, 2026))
                .thenReturn(new BigDecimal("600000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2026))
                .thenReturn(new BigDecimal("400000"));

        MonthSummary result = summaryService.getSummary(USER_ID, 5, 2026);

        assertThat(result.getStatus()).isEqualTo(MonthStatus.ABUNDANCE);
    }

    @Test
    @DisplayName("getSummary - LEAN when income < average * 0.85")
    void getSummary_lean() {
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 5, 2026))
                .thenReturn(new BigDecimal("200000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2026))
                .thenReturn(new BigDecimal("400000"));

        MonthSummary result = summaryService.getSummary(USER_ID, 5, 2026);

        assertThat(result.getStatus()).isEqualTo(MonthStatus.LEAN);
    }

    @Test
    @DisplayName("getSummary - percentageVsAverage calculated correctly")
    void getSummary_percentageCalculation() {
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 5, 2026))
                .thenReturn(new BigDecimal("500000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2026))
                .thenReturn(new BigDecimal("400000"));
        when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2026))
                .thenReturn(new BigDecimal("400000"));

        MonthSummary result = summaryService.getSummary(USER_ID, 5, 2026);

        assertThat(result.getPercentageVsAverage()).isEqualTo(25.0);
    }
}
