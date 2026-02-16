package com.velocity.carservice.application.strategy;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BankTransferPaymentStrategy implements PaymentStrategy {

    @Override
    public BookingStatus processPayment(Booking booking, String paymentReference) {
        log.info("Processing bank transfer payment for booking: {}", booking.getBookingId());
        log.info("Bank transfer payment - creating booking with PENDING_PAYMENT status");
        return BookingStatus.PENDING_PAYMENT;
    }

    @Override
    public PaymentMode getPaymentMode() {
        return PaymentMode.BANK_TRANSFER;
    }
}

