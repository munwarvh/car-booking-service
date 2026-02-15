package com.velocity.carservice.application.strategy;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;

/**
 * Strategy interface for processing different payment modes.
 * Implementations handle the specific logic for each payment type.
 */
public interface PaymentStrategy {

    /**
     * Process the payment and determine the booking status.
     *
     * @param booking the booking entity
     * @param paymentReference the payment reference (e.g., credit card token, transaction ID)
     * @return the resulting booking status after payment processing
     */
    BookingStatus processPayment(Booking booking, String paymentReference);

    /**
     * Returns the payment mode this strategy handles.
     *
     * @return the supported payment mode
     */
    PaymentMode getPaymentMode();
}

