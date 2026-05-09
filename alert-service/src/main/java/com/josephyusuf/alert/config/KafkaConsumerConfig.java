package com.josephyusuf.alert.config;

import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.dto.RuleAppliedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:alert-service}")
    private String groupId;

    private <T> ConsumerFactory<String, T> buildConsumerFactory(Class<T> targetType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, IncomeClassifiedEvent> incomeClassifiedConsumerFactory() {
        return buildConsumerFactory(IncomeClassifiedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IncomeClassifiedEvent> incomeClassifiedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, IncomeClassifiedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(incomeClassifiedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, RuleAppliedEvent> ruleAppliedConsumerFactory() {
        return buildConsumerFactory(RuleAppliedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RuleAppliedEvent> ruleAppliedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RuleAppliedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ruleAppliedConsumerFactory());
        return factory;
    }
}
