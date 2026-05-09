package com.josephyusuf.income.producer;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.event.IncomeClassifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncomeEventProducer {

    public static final String TOPIC = "income.classified";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishIncomeClassified(MonthSummary summary) {
        IncomeClassifiedEvent event = IncomeClassifiedEvent.builder()
                .userId(summary.getUserId())
                .month(summary.getMonth())
                .year(summary.getYear())
                .totalIncome(summary.getTotalIncome())
                .averageLast3Months(summary.getAverageLast3Months())
                .status(summary.getStatus().name())
                .percentageVsAverage(summary.getPercentageVsAverage())
                .occurredAt(Instant.now())
                .build();

        try {
            kafkaTemplate.send(TOPIC, summary.getUserId().toString(), event);
            log.info("IncomeClassifiedEvent publié userId={} status={} {}/{}",
                    event.getUserId(), event.getStatus(), event.getMonth(), event.getYear());
        } catch (Exception e) {
            log.error("Échec publication IncomeClassifiedEvent : {}", e.getMessage());
        }
    }
}
