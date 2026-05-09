package com.josephyusuf.ruleengine.producer;

import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;
import com.josephyusuf.ruleengine.event.RuleAppliedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RuleEventProducer producer;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Integer MONTH = 5;
    private static final Integer YEAR = 2026;

    private AllocationResult buildResult() {
        return AllocationResult.builder()
                .rule(RuleType.RULE_JOSEPH)
                .totalIncome(new BigDecimal("750000"))
                .monthStatus("ABUNDANCE")
                .message("Mois d'abondance")
                .build();
    }

    @Test
    @DisplayName("publishRuleApplied envoie l'evenement sur le topic rule.applied avec userId comme cle")
    void publishRuleApplied_sendsEventOnCorrectTopic() {
        AllocationResult result = buildResult();

        producer.publishRuleApplied(USER_ID, result, MONTH, YEAR);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RuleAppliedEvent> eventCaptor = ArgumentCaptor.forClass(RuleAppliedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(RuleEventProducer.TOPIC);
        assertThat(topicCaptor.getValue()).isEqualTo("rule.applied");
        assertThat(keyCaptor.getValue()).isEqualTo(USER_ID.toString());

        RuleAppliedEvent published = eventCaptor.getValue();
        assertThat(published.getUserId()).isEqualTo(USER_ID);
        assertThat(published.getRule()).isEqualTo(RuleType.RULE_JOSEPH.name());
        assertThat(published.getTotalIncome()).isEqualByComparingTo("750000");
        assertThat(published.getMonthStatus()).isEqualTo("ABUNDANCE");
        assertThat(published.getMonth()).isEqualTo(MONTH);
        assertThat(published.getYear()).isEqualTo(YEAR);
        assertThat(published.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("publishRuleApplied n'echoue pas si KafkaTemplate leve une exception")
    void publishRuleApplied_swallowsKafkaException() {
        doThrow(new RuntimeException("broker down"))
                .when(kafkaTemplate).send(eq(RuleEventProducer.TOPIC), any(String.class), any(RuleAppliedEvent.class));

        assertThatCode(() -> producer.publishRuleApplied(USER_ID, buildResult(), MONTH, YEAR))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send(eq(RuleEventProducer.TOPIC), eq(USER_ID.toString()), any(RuleAppliedEvent.class));
    }
}
