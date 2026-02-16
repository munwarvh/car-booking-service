package com.velocity.carservice.application.dto.event;

import java.math.BigDecimal;

public record BankTransferPaymentEvent(
        String paymentId,
        String senderAccountNumber,
        BigDecimal paymentAmount,
        String transactionDetails
) {
    /**
     * Extracts the booking ID from transactionDetails
     * Format: <TxnRef (12 chars)> <BookingId (10 chars)>
     */
    public String extractBookingId() {
        if (transactionDetails == null || transactionDetails.length() < 23) {
            return null;
        }
        return transactionDetails.substring(13).trim();
    }

    /**
     * Extracts the transaction reference from transactionDetails
     */
    public String extractTransactionReference() {
        if (transactionDetails == null || transactionDetails.length() < 12) {
            return null;
        }
        return transactionDetails.substring(0, 12);
    }
}
