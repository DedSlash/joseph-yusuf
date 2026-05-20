package com.josephyusuf.alert.consumer;

import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.dto.RuleAppliedEvent;
import com.josephyusuf.alert.dto.SavingsRecommendationEvent;
import com.josephyusuf.alert.service.AlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaConsumersTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private IncomeClassifiedConsumer incomeClassifiedConsumer;

    @InjectMocks
    private RuleAppliedConsumer ruleAppliedConsumer;

    @InjectMocks
    private SavingsRecommendationConsumer savingsRecommendationConsumer;

    @Test
    void incomeClassifiedConsumer_delegatesToService() {
        IncomeClassifiedEvent event = IncomeClassifiedEvent.builder()
                .userId(UUID.randomUUID())
                .month(5)
                .year(2026)
                .totalIncome(new BigDecimal("3000"))
                .averageLast3Months(new BigDecimal("2000"))
                .status("ABUNDANCE")
                .percentageVsAverage(50.0)
                .occurredAt(Instant.now())
                .build();

        incomeClassifiedConsumer.onIncomeClassified(event);

        verify(alertService).createFromIncomeClassified(event);
    }

    @Test
    void ruleAppliedConsumer_delegatesToService() {
        RuleAppliedEvent event = RuleAppliedEvent.builder()
                .userId(UUID.randomUUID())
                .rule("RULE_50_30_20")
                .totalIncome(new BigDecimal("2500"))
                .monthStatus("NORMAL")
                .month(5)
                .year(2026)
                .occurredAt(Instant.now())
                .build();

        ruleAppliedConsumer.onRuleApplied(event);

        verify(alertService).createFromRuleApplied(event);
    }

    @Test
    void savingsRecommendationConsumer_delegatesToService() {
        SavingsRecommendationEvent event = SavingsRecommendationEvent.builder()
                .userId(UUID.randomUUID())
                .goalId(UUID.randomUUID())
                .goalName("Voyage")
                .recommendedAmount(new BigDecimal("50000"))
                .josephStatus("ABUNDANCE")
                .message("Versement recommandé")
                .month(5)
                .year(2026)
                .occurredAt(Instant.now())
                .build();

        savingsRecommendationConsumer.onSavingsRecommendation(event);

        verify(alertService).createFromSavingsRecommendation(event);
    }
}
