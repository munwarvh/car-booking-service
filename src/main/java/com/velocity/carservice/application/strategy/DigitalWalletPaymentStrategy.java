package com.velocity.carservice.application.strategy;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Payment strategy for Digital Wallet payments.
 * Digital wallet payments are pre-authorized and confirmed immediately.
 */
@Component
@Slf4j
public class DigitalWalletPaymentStrategy implements PaymentStrategy {

    @Override
    public BookingStatus processPayment(Booking booking, String paymentReference) {
        log.info("Processing digital wallet payment for booking: {}", booking.getBookingId());
        log.info("Digital wallet payment - confirming booking immediately");
        return BookingStatus.CONFIRMED;
    }

    @Override
    public PaymentMode getPaymentMode() {
        return PaymentMode.DIGITAL_WALLET;
    }
}

