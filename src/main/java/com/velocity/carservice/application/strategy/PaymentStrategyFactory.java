package com.velocity.carservice.application.strategy;

import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.UnsupportedPaymentModeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for resolving the appropriate PaymentStrategy based on payment mode.
 * Uses Spring's dependency injection to automatically register all strategy implementations.
 */
@Component
@Slf4j
public class PaymentStrategyFactory {

    private final Map<PaymentMode, PaymentStrategy> strategies;

    /**
     * Constructor that auto-wires all PaymentStrategy implementations.
     * Each strategy is registered in a map keyed by its payment mode.
     *
     * @param paymentStrategies list of all PaymentStrategy beans
     */
    public PaymentStrategyFactory(List<PaymentStrategy> paymentStrategies) {
        strategies = new EnumMap<>(PaymentMode.class);
        paymentStrategies.forEach(strategy -> {
            strategies.put(strategy.getPaymentMode(), strategy);
            log.info("Registered payment strategy: {} for mode: {}",
                    strategy.getClass().getSimpleName(), strategy.getPaymentMode());
        });
    }

    /**
     * Get the appropriate payment strategy for the given payment mode.
     *
     * @param paymentMode the payment mode
     * @return the corresponding payment strategy
     * @throws UnsupportedPaymentModeException if no strategy is registered for the payment mode
     */
    public PaymentStrategy getStrategy(PaymentMode paymentMode) {
        PaymentStrategy strategy = strategies.get(paymentMode);
        if (strategy == null) {
            log.error("No payment strategy found for payment mode: {}", paymentMode);
            throw new UnsupportedPaymentModeException("Unsupported payment mode: " + paymentMode);
        }
        return strategy;
    }
}
