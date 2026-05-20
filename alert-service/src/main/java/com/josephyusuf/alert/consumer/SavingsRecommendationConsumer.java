package com.josephyusuf.alert.consumer;

import com.josephyusuf.alert.dto.SavingsRecommendationEvent;
import com.josephyusuf.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsRecommendationConsumer {

    public static final String TOPIC = "joseph.savings.recommendation";

    private final AlertService alertService;

    @KafkaListener(topics = TOPIC, groupId = "alert-service", containerFactory = "savingsRecommendationListenerFactory")
    public void onSavingsRecommendation(SavingsRecommendationEvent event) {
        log.info("Réception SavingsRecommendationEvent userId={} goalId={} status={} {}/{}",
                event.getUserId(), event.getGoalId(), event.getJosephStatus(),
                event.getMonth(), event.getYear());
        alertService.createFromSavingsRecommendation(event);
    }
}
