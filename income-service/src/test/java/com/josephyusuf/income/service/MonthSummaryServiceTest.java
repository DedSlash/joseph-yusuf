package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthSummaryServiceTest {

    @Mock
    private IncomeEntryRepository entryRepository;

    @InjectMocks
    private MonthSummaryService monthSummaryService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("getSummary - calcul de la moyenne et des seuils")
    class GetSummaryCalculations {

        @Test
        @DisplayName("Avec 3 mois d'historique, calcule la moyenne correcte et les seuils")
        void withThreeMonthsHistory_calculatesCorrectAverageAndThresholds() {
            // Given: current month is April 2024, previous 3 months have income
            int month = 4;
            int year = 2024;

            // Current month income (between lean 85000 and abundance 115000 thresholds)
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("105000"));
            // Previous 3 months
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("110000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("90000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            // Average = (100000 + 110000 + 90000) / 3 = 100000.00
            BigDecimal expectedAverage = new BigDecimal("100000.00");
            BigDecimal expectedAbundance = expectedAverage.multiply(new BigDecimal("1.15"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedLean = expectedAverage.multiply(new BigDecimal("0.85"))
                    .setScale(2, RoundingMode.HALF_UP);

            assertThat(summary.getUserId()).isEqualTo(USER_ID);
            assertThat(summary.getMonth()).isEqualTo(4);
            assertThat(summary.getYear()).isEqualTo(2024);
            assertThat(summary.getTotalIncome()).isEqualByComparingTo(new BigDecimal("105000"));
            assertThat(summary.getAverageLast3Months()).isEqualByComparingTo(expectedAverage);
            assertThat(summary.getAbundanceThreshold()).isEqualByComparingTo(expectedAbundance);
            assertThat(summary.getLeanThreshold()).isEqualByComparingTo(expectedLean);
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
        }

        @Test
        @DisplayName("Un seul mois d'historique utilise ce mois comme base")
        void withSingleMonthHistory_usesThatMonthAsBase() {
            // Given: only one previous month has income
            int month = 3;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("150000"));
            // Only February has income
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            // January and December have zero
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(BigDecimal.ZERO);
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 12, 2023))
                    .thenReturn(BigDecimal.ZERO);

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then: average is based only on February = 100000
            assertThat(summary.getAverageLast3Months()).isEqualByComparingTo(new BigDecimal("100000.00"));
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.ABUNDANCE);
        }

        @Test
        @DisplayName("Pas d'historique (average=0) retourne NORMAL et percentageVsAverage=0.0")
        void withNoHistory_returnsNormalAndZeroPercentage() {
            // Given: no previous months have income
            int month = 1;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("50000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 12, 2023))
                    .thenReturn(BigDecimal.ZERO);
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 11, 2023))
                    .thenReturn(BigDecimal.ZERO);
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 10, 2023))
                    .thenReturn(BigDecimal.ZERO);

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getAverageLast3Months()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
            assertThat(summary.getPercentageVsAverage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Calcul du percentageVsAverage correct")
        void percentageVsAverage_calculatedCorrectly() {
            // Given: average=100000, totalIncome=120000 => +20%
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("120000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then: (120000 - 100000) / 100000 * 100 = 20.0
            assertThat(summary.getPercentageVsAverage()).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("getSummary - determination du statut ABUNDANCE")
    class AbundanceStatus {

        @Test
        @DisplayName("ABUNDANCE quand totalIncome strictement > average * 1.15")
        void abundance_whenIncomeStrictlyAboveThreshold() {
            // Given: average=100000, totalIncome=115001 (strictly > 115000)
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("115001"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.ABUNDANCE);
        }
    }

    @Nested
    @DisplayName("getSummary - determination du statut LEAN")
    class LeanStatus {

        @Test
        @DisplayName("LEAN quand totalIncome strictement < average * 0.85")
        void lean_whenIncomeStrictlyBelowThreshold() {
            // Given: average=100000, totalIncome=84999 (strictly < 85000)
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("84999"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.LEAN);
        }
    }

    @Nested
    @DisplayName("getSummary - statut NORMAL entre les seuils")
    class NormalStatus {

        @Test
        @DisplayName("NORMAL quand totalIncome entre les deux seuils")
        void normal_whenIncomeBetweenThresholds() {
            // Given: average=100000, totalIncome=100000 (within thresholds)
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
        }
    }

    @Nested
    @DisplayName("getSummary - tests de bordure critiques (compareTo strict)")
    class BoundaryTests {

        @Test
        @DisplayName("NORMAL quand totalIncome == average * 1.15 exactement (pas ABUNDANCE)")
        void exactlyAtAbundanceThreshold_isNormal() {
            // Given: average=100000, totalIncome=115000 (exactly 1.15x)
            // compareTo == 0, NOT > 0, so NOT ABUNDANCE
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("115000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then: exactly at threshold = NORMAL (strictly greater required for ABUNDANCE)
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
        }

        @Test
        @DisplayName("ABUNDANCE quand totalIncome == average * 1.15 + 1 (juste au-dessus)")
        void justAboveAbundanceThreshold_isAbundance() {
            // Given: average=100000, totalIncome=115001
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("115001"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.ABUNDANCE);
        }

        @Test
        @DisplayName("NORMAL quand totalIncome == average * 0.85 exactement (pas LEAN)")
        void exactlyAtLeanThreshold_isNormal() {
            // Given: average=100000, totalIncome=85000 (exactly 0.85x)
            // compareTo == 0, NOT < 0, so NOT LEAN
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("85000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then: exactly at threshold = NORMAL (strictly less required for LEAN)
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
        }

        @Test
        @DisplayName("LEAN quand totalIncome == average * 0.85 - 1 (juste en dessous)")
        void justBelowLeanThreshold_isLean() {
            // Given: average=100000, totalIncome=84999
            int month = 4;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 4, 2024))
                    .thenReturn(new BigDecimal("84999"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 3, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 2, 2024))
                    .thenReturn(new BigDecimal("100000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.LEAN);
        }
    }

    @Nested
    @DisplayName("getSummary - gestion du passage d'annee")
    class YearWrapAround {

        @Test
        @DisplayName("Passage d'annee correct pour janvier (mois precedents en annee anterieure)")
        void january_wrapsToDecemberNovemberOctober() {
            // Given: current month is January 2024
            int month = 1;
            int year = 2024;

            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 1, 2024))
                    .thenReturn(new BigDecimal("100000"));
            // Previous months wrap to 2023
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 12, 2023))
                    .thenReturn(new BigDecimal("90000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 11, 2023))
                    .thenReturn(new BigDecimal("110000"));
            when(entryRepository.sumByUserIdAndMonthAndYear(USER_ID, 10, 2023))
                    .thenReturn(new BigDecimal("100000"));

            // When
            MonthSummary summary = monthSummaryService.getSummary(USER_ID, month, year);

            // Then: average = (90000 + 110000 + 100000) / 3 = 100000
            assertThat(summary.getAverageLast3Months()).isEqualByComparingTo(new BigDecimal("100000.00"));
            assertThat(summary.getStatus()).isEqualTo(MonthStatus.NORMAL);
        }
    }
}
