package com.velocity.carservice.infrastructure.adapter.outbound.rest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardValidationClient {

    private final WebClient webClient;

    @Value("${app.external-services.credit-card-validation.url:http://localhost:9090}")
    private String baseUrl;

    @Value("${app.external-services.credit-card-validation.timeout:5000}")
    private int timeout;

    private static final String CREDIT_CARD_SERVICE = "creditCardService";

    /**
     * Validates credit card payment by calling external credit-card-validation-service
     * @param paymentReference the payment reference to validate
     * @return true if payment status is APPROVED, false otherwise
     */
    @CircuitBreaker(name = CREDIT_CARD_SERVICE, fallbackMethod = "validatePaymentFallback")
    @Retry(name = CREDIT_CARD_SERVICE)
    public boolean validatePayment(String paymentReference) {
        log.info("Validating credit card payment with reference: {}", paymentReference);

        try {
            PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest(paymentReference);

            PaymentStatusResponse response = webClient.post()
                    .uri(baseUrl + "/payment-status")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("Credit card validation failed with status: {}", clientResponse.statusCode());
                        return Mono.error(new CreditCardValidationException("Payment validation failed: " + clientResponse.statusCode()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.error("Credit card service error: {}", clientResponse.statusCode());
                        return Mono.error(new CreditCardValidationException("Credit card service unavailable"));
                    })
                    .bodyToMono(PaymentStatusResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response != null && "APPROVED".equals(response.status())) {
                log.info("Credit card payment APPROVED for reference: {}", paymentReference);
                return true;
            }

            log.warn("Credit card payment not approved. Status: {}", response != null ? response.status() : "null");
            return false;

        } catch (Exception e) {
            log.error("Credit card validation error for reference {}: {}", paymentReference, e.getMessage());
            throw new CreditCardValidationException("Failed to validate credit card payment", e);
        }
    }

    @SuppressWarnings("unused")
    private boolean validatePaymentFallback(String paymentReference, Exception ex) {
        log.error("Credit card validation fallback triggered for reference {}: {}", paymentReference, ex.getMessage());
        throw new CreditCardValidationException("Credit card validation service is currently unavailable", ex);
    }

    /**
     * Request DTO as per OpenAPI spec
     */
    public record PaymentStatusRetrievalRequest(String paymentReference) {}

    /**
     * Response DTO as per OpenAPI spec
     */
    public record PaymentStatusResponse(
            LocalDateTime lastUpdateDate,
            String status  // APPROVED or REJECTED
    ) {}

    /**
     * Custom exception for credit card validation errors
     */
    public static class CreditCardValidationException extends RuntimeException {
        public CreditCardValidationException(String message) {
            super(message);
        }

        public CreditCardValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
