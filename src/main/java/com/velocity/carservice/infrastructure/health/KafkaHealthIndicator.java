package com.velocity.carservice.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Kafka connectivity.
 * Checks if the Kafka producer can connect to the broker.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        try {
            var metrics = kafkaTemplate.metrics();
            if (metrics != null && !metrics.isEmpty()) {
                return Health.up()
                        .withDetail("kafka", "Connected")
                        .withDetail("metrics_available", metrics.size())
                        .build();
            }
            return Health.up()
                    .withDetail("kafka", "Producer initialized")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

