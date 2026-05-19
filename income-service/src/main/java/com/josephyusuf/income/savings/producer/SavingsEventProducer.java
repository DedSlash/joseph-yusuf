package com.josephyusuf.income.savings.producer;

import com.josephyusuf.income.savings.dto.SavingsRecommendationDto;
import com.josephyusuf.income.savings.event.SavingsRecommendationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsEventProducer {

    public static final String TOPIC = "joseph.savings.recommendation";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRecommendations(UUID userId, List<SavingsRecommendationDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        for (SavingsRecommendationDto reco : recommendations) {
            SavingsRecommendationEvent event = SavingsRecommendationEvent.builder()
                    .userId(userId)
                    .goalId(reco.getGoalId())
                    .goalName(reco.getGoalName())
                    .recommendedAmount(reco.getRecommendedAmount())
                    .josephStatus(reco.getJosephStatus() != null ? reco.getJosephStatus().name() : null)
                    .message(reco.getMessage())
                    .month(reco.getMonth())
                    .year(reco.getYear())
                    .occurredAt(Instant.now())
                    .build();
            try {
                kafkaTemplate.send(TOPIC, userId.toString(), event);
                log.info("SavingsRecommendationEvent publié userId={} goalId={} status={} {}/{}",
                        userId, reco.getGoalId(), event.getJosephStatus(), event.getMonth(), event.getYear());
            } catch (Exception e) {
                log.error("Échec publication SavingsRecommendationEvent : {}", e.getMessage());
            }
        }
    }
}
