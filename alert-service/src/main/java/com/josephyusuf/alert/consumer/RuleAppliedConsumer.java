package com.josephyusuf.alert.consumer;

import com.josephyusuf.alert.dto.RuleAppliedEvent;
import com.josephyusuf.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleAppliedConsumer {

    public static final String TOPIC = "rule.applied";

    private final AlertService alertService;

    @KafkaListener(topics = TOPIC, groupId = "alert-service", containerFactory = "ruleAppliedListenerFactory")
    public void onRuleApplied(RuleAppliedEvent event) {
        log.info("Réception RuleAppliedEvent userId={} rule={}", event.getUserId(), event.getRule());
        alertService.createFromRuleApplied(event);
    }
}
