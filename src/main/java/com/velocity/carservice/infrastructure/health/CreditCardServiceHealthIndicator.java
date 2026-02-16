package com.velocity.carservice.infrastructure.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Custom health indicator for Credit Card Validation Service.
 * Checks if the external credit card service is reachable.
 */
@Component
public class CreditCardServiceHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private final String creditCardServiceUrl;

    public CreditCardServiceHealthIndicator(
            WebClient.Builder webClientBuilder,
            @Value("${app.external-services.credit-card-validation.url:http://localhost:8081}") String creditCardServiceUrl) {
        this.webClient = webClientBuilder.build();
        this.creditCardServiceUrl = creditCardServiceUrl;
    }

    @Override
    public Health health() {
        try {
            webClient.head()
                    .uri(creditCardServiceUrl + "/health")
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(3));

            return Health.up()
                    .withDetail("creditCardService", "Available")
                    .withDetail("url", creditCardServiceUrl)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("creditCardService", "Unavailable")
                    .withDetail("url", creditCardServiceUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

