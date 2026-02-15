package com.velocity.carservice.application.strategy;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.infrastructure.adapter.outbound.rest.CreditCardValidationClient;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.PaymentFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Payment strategy for Credit Card payments.
 * Validates the credit card with an external credit card validation service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardPaymentStrategy implements PaymentStrategy {

    private final CreditCardValidationClient creditCardValidationClient;

    @Override
    public BookingStatus processPayment(Booking booking, String paymentReference) {
        log.info("Processing credit card payment for booking: {}", booking.getBookingId());
        log.info("Credit card payment - validating with external service");

        boolean isApproved = creditCardValidationClient.validatePayment(paymentReference);

        if (isApproved) {
            log.info("Credit card payment APPROVED for booking {}", booking.getBookingId());
            return BookingStatus.CONFIRMED;
        } else {
            log.error("Credit card payment REJECTED for booking {}", booking.getBookingId());
            throw new PaymentFailedException("Credit card payment was not approved");
        }
    }

    @Override
    public PaymentMode getPaymentMode() {
        return PaymentMode.CREDIT_CARD;
    }
}

