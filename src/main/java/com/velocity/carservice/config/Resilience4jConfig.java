package com.velocity.carservice.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;


@Configuration
public class Resilience4jConfig {

    /**
     * Customizer for creditCardService circuit breaker.
     * Adds specific exception recording that's complex to configure in YAML.
     */
    @Bean
    public CircuitBreakerConfigCustomizer creditCardServiceCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer.of("creditCardService", builder ->
                builder.recordExceptions(
                        IOException.class,
                        TimeoutException.class,
                        ConnectException.class,
                        SocketTimeoutException.class,
                        WebClientRequestException.class,
                        WebClientResponseException.InternalServerError.class,
                        WebClientResponseException.BadGateway.class,
                        WebClientResponseException.ServiceUnavailable.class,
                        WebClientResponseException.GatewayTimeout.class
                ).ignoreExceptions(
                        WebClientResponseException.BadRequest.class,
                        WebClientResponseException.NotFound.class,
                        WebClientResponseException.Unauthorized.class,
                        WebClientResponseException.Forbidden.class,
                        IllegalArgumentException.class
                )
        );
    }
}
