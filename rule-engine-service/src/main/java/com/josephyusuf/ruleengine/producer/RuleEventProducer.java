package com.josephyusuf.ruleengine.producer;

import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.event.RuleAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEventProducer {

    public static final String TOPIC = "rule.applied";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRuleApplied(UUID userId, AllocationResult result, Integer month, Integer year) {
        RuleAppliedEvent event = RuleAppliedEvent.builder()
                .userId(userId)
                .rule(result.getRule().name())
                .totalIncome(result.getTotalIncome())
                .monthStatus(result.getMonthStatus())
                .month(month)
                .year(year)
                .occurredAt(Instant.now())
                .build();

        try {
            kafkaTemplate.send(TOPIC, userId.toString(), event);
            log.info("RuleAppliedEvent publié userId={} rule={}", userId, event.getRule());
        } catch (Exception e) {
            log.error("Échec publication RuleAppliedEvent : {}", e.getMessage());
        }
    }
}
