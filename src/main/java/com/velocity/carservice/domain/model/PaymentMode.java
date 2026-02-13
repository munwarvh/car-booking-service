package com.velocity.carservice.domain.model;

/**
 * Payment modes supported for car rental bookings.
 *
 * DIGITAL_WALLET: Immediate confirmation (as per requirement "digital wallet")
 * CREDIT_CARD: Requires validation with credit-card-validation-service
 * BANK_TRANSFER: Creates booking with PENDING_PAYMENT status, confirmed via Kafka events
 */
public enum PaymentMode {
    DIGITAL_WALLET,
    CREDIT_CARD,
    BANK_TRANSFER
}
