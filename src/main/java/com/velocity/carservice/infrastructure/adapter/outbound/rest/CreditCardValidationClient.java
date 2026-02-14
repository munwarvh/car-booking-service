package com.velocity.carservice.infrastructure.adapter.outbound.rest;

import com.velocity.carservice.infrastructure.adapter.outbound.creditcard.api.DefaultApi;
import com.velocity.carservice.infrastructure.adapter.outbound.creditcard.model.PaymentStatusRetrievalRequest;
import com.velocity.carservice.infrastructure.adapter.outbound.creditcard.model.PaymentStatusResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
@Slf4j
public class CreditCardValidationClient {

    private final DefaultApi creditCardApi;
    private final int timeout;

    private static final String CREDIT_CARD_SERVICE = "creditCardService";

    public CreditCardValidationClient(
            WebClient webClient,
            @Value("${app.external-services.credit-card-validation.url:http://localhost:9090}") String baseUrl,
            @Value("${app.external-services.credit-card-validation.base-path:/host/credit-card-payment-api}") String basePath,
            @Value("${app.external-services.credit-card-validation.timeout:5000}") int timeout) {
        this.timeout = timeout;

        // Create the generated API client with the configured WebClient
        com.velocity.carservice.infrastructure.adapter.outbound.creditcard.ApiClient apiClient =
                new com.velocity.carservice.infrastructure.adapter.outbound.creditcard.ApiClient(webClient);
        apiClient.setBasePath(baseUrl + basePath);
        this.creditCardApi = new DefaultApi(apiClient);
    }

    /**
     * Validates credit card payment by calling external credit-card-validation-service
     *
     * @param paymentReference the payment reference to validate
     * @return true if payment status is APPROVED, false otherwise
     */
    @CircuitBreaker(name = CREDIT_CARD_SERVICE, fallbackMethod = "validatePaymentFallback")
    @Retry(name = CREDIT_CARD_SERVICE)
    public boolean validatePayment(String paymentReference) {
        log.info("Validating credit card payment with reference: {}", paymentReference);

        try {
            PaymentStatusRetrievalRequest request = new PaymentStatusRetrievalRequest();
            request.setPaymentReference(paymentReference);

            PaymentStatusResponse response = creditCardApi.getPaymentStatus(request)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response != null && PaymentStatusResponse.StatusEnum.APPROVED.equals(response.getStatus())) {
                log.info("Credit card payment APPROVED for reference: {}", paymentReference);
                return true;
            }

            log.warn("Credit card payment not approved. Status: {}", response != null ? response.getStatus() : "null");
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
