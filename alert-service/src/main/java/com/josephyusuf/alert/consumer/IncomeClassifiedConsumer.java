package com.josephyusuf.alert.consumer;

import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncomeClassifiedConsumer {

    public static final String TOPIC = "income.classified";

    private final AlertService alertService;

    @KafkaListener(topics = TOPIC, groupId = "alert-service", containerFactory = "incomeClassifiedListenerFactory")
    public void onIncomeClassified(IncomeClassifiedEvent event) {
        log.info("Réception IncomeClassifiedEvent userId={} status={} month={}/{}",
                event.getUserId(), event.getStatus(), event.getMonth(), event.getYear());
        alertService.createFromIncomeClassified(event);
    }
}
