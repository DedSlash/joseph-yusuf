package com.josephyusuf.income.tips;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.service.MonthSummaryService;
import com.josephyusuf.income.tips.dto.MoneyTipDto;
import com.josephyusuf.income.tips.dto.MoneyTipsDto;
import com.josephyusuf.income.tips.service.MoneyTipsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyTipsServiceTest {

    @Mock
    private MonthSummaryService monthSummaryService;

    @InjectMocks
    private MoneyTipsService moneyTipsService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // No-op : chaque test stubbe la summary qu'il lui faut.
    }

    private void stubSummary(MonthStatus status, BigDecimal income, BigDecimal average3) {
        MonthSummary summary = MonthSummary.builder()
                .userId(USER_ID)
                .month(4)
                .year(2026)
                .totalIncome(income)
                .averageLast3Months(average3)
                .abundanceThreshold(average3.multiply(new BigDecimal("1.15")))
                .leanThreshold(average3.multiply(new BigDecimal("0.85")))
                .status(status)
                .percentageVsAverage(0.0)
                .monthsInBaseline(3)
                .build();
        when(monthSummaryService.getSummary(any(UUID.class), anyInt(), anyInt())).thenReturn(summary);
    }

    @Test
    @DisplayName("Pays SN : Wave inclus, Likelemba (CM) exclu, BRVM inclus")
    void filterByCountry_senegalIncludesWaveExcludesLikelemba() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getTips()).extracting(MoneyTipDto::getId)
                .contains("TIP_001", "TIP_002", "TIP_003", "TIP_004")
                .doesNotContain("TIP_005", "TIP_006");
    }

    @Test
    @DisplayName("Pays CM : Likelemba inclus, Wave exclu")
    void filterByCountry_cameroonIncludesLikelembaExcludesWave() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "CM", "XAF", Locale.FRENCH);

        assertThat(result.getTips()).extracting(MoneyTipDto::getId)
                .contains("TIP_004", "TIP_005")
                .doesNotContain("TIP_003", "TIP_006");
    }

    @Test
    @DisplayName("Pays FR : tips bancaires européens inclus, tips Afrique exclus")
    void filterByCountry_franceIncludesBank() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("3000"), new BigDecimal("3000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "FR", "EUR", Locale.FRENCH);

        assertThat(result.getTips()).extracting(MoneyTipDto::getId)
                .contains("TIP_006")
                .doesNotContain("TIP_003", "TIP_004", "TIP_005");
    }

    @Test
    @DisplayName("Plan FREE : tips PREMIUM et PREMIUM_PLUS marqués locked=true")
    void freePlan_locksPremiumTips() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "SN", "XOF", Locale.FRENCH);

        MoneyTipDto premium = result.getTips().stream().filter(t -> "TIP_007".equals(t.getId())).findFirst().orElseThrow();
        MoneyTipDto premiumPlus = result.getTips().stream().filter(t -> "TIP_009".equals(t.getId())).findFirst().orElseThrow();
        MoneyTipDto free = result.getTips().stream().filter(t -> "TIP_001".equals(t.getId())).findFirst().orElseThrow();

        assertThat(premium.isLocked()).isTrue();
        assertThat(premiumPlus.isLocked()).isTrue();
        assertThat(free.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Plan PREMIUM_PLUS : aucun tip locked")
    void premiumPlusPlan_nothingLocked() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM_PLUS", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getTips()).extracting(MoneyTipDto::isLocked).containsOnly(false);
    }

    @Test
    @DisplayName("Mois LEAN : tips investissement (TIP_009, TIP_010) masqués, TIP_001 en tête")
    void leanMonth_hidesInvestmentAndPrioritizesEnvelopes() {
        stubSummary(MonthStatus.LEAN, new BigDecimal("100000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM_PLUS", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getTips()).extracting(MoneyTipDto::getId)
                .doesNotContain("TIP_009", "TIP_010");
        assertThat(result.getTips().get(0).getId()).isEqualTo("TIP_001");
    }

    @Test
    @DisplayName("Mois ABUNDANCE : tips avancés (007/008/009) en premier")
    void abundanceMonth_advancedTipsFirst() {
        stubSummary(MonthStatus.ABUNDANCE, new BigDecimal("800000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM_PLUS", "SN", "XOF", Locale.FRENCH);

        String firstId = result.getTips().get(0).getId();
        assertThat(firstId).isIn("TIP_007", "TIP_008", "TIP_009");
    }

    @Test
    @DisplayName("TIP_008 : description personnalisée avec recommendedSavings et devise")
    void tip008_descriptionContainsRecommendedAmount() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM", "SN", "XOF", Locale.FRENCH);

        MoneyTipDto tip008 = result.getTips().stream()
                .filter(t -> "TIP_008".equals(t.getId()))
                .findFirst()
                .orElseThrow();

        // Pour NORMAL avec revenu 500000, recommendedSavings = 20% = 100000.00
        assertThat(tip008.getDescription()).contains("100000.00");
        assertThat(tip008.getDescription()).contains("XOF");
        assertThat(tip008.getDescription()).doesNotContain("{recommendedSavings}");
        assertThat(tip008.getDescription()).doesNotContain("{currency}");
    }

    @Test
    @DisplayName("LEAN : recommendedSavings = 0")
    void leanMonth_recommendsZeroSavings() {
        stubSummary(MonthStatus.LEAN, new BigDecimal("200000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getRecommendedSavings()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ABUNDANCE : recommendedSavings = surplus + 20% du revenu")
    void abundanceMonth_recommendsSurplusPlusBase() {
        // Income 800000, avg 500000 → surplus 300000, base 20% × 800000 = 160000, total 460000
        stubSummary(MonthStatus.ABUNDANCE, new BigDecimal("800000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "PREMIUM", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getRecommendedSavings()).isEqualByComparingTo(new BigDecimal("460000.00"));
    }

    @Test
    @DisplayName("Locale EN : titres et descriptions en anglais")
    void englishLocale_returnsEnglishContent() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("500000"), new BigDecimal("500000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "SN", "XOF", Locale.ENGLISH);

        MoneyTipDto envelope = result.getTips().stream().filter(t -> "TIP_001".equals(t.getId())).findFirst().orElseThrow();
        assertThat(envelope.getTitle()).isEqualTo("The envelope system");
        assertThat(envelope.getDescription()).contains("envelopes");
    }

    @Test
    @DisplayName("Répartition 50/30/20 calculée correctement")
    void splitRespects50_30_20() {
        stubSummary(MonthStatus.NORMAL, new BigDecimal("1000000"), new BigDecimal("1000000"));

        MoneyTipsDto result = moneyTipsService.getTips(USER_ID, 4, 2026, "FREE", "SN", "XOF", Locale.FRENCH);

        assertThat(result.getRecommendedSplit().getNeeds()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getRecommendedSplit().getWants()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(result.getRecommendedSplit().getSavings()).isEqualByComparingTo(new BigDecimal("200000.00"));
    }
}
