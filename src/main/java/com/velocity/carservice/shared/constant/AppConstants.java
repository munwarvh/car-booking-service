package com.velocity.carservice.shared.constant;

/**
 * Application-wide constants for the Car Booking Service.
 */
public final class AppConstants {

    private AppConstants() {
    }

    // API Versioning
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;

    // Booking Constants
    public static final String BOOKING_ID_PREFIX = "BKG";
    public static final int MAX_RENTAL_DAYS = 21;
    public static final int BANK_TRANSFER_CANCELLATION_HOURS_BEFORE_RENTAL = 48;

    // Kafka Topics
    public static final String TOPIC_BANK_TRANSFER_PAYMENT_EVENTS = "bank-transfer-payment-events";
    public static final String TOPIC_BANK_TRANSFER_PAYMENT_EVENTS_DLQ = "bank-transfer-payment-events-dlq";

    // Kafka Consumer Groups
    public static final String CONSUMER_GROUP_CAR_BOOKING_SERVICE = "car-booking-service-group";

    // External Services
    public static final String CREDIT_CARD_SERVICE_NAME = "creditCardService";

    // HTTP Headers
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    // Error Codes
    public static final String ERROR_BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
    public static final String ERROR_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ERROR_PAYMENT_REJECTED = "PAYMENT_REJECTED";
    public static final String ERROR_INVALID_STATE = "INVALID_STATE";
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

    // Payment Status
    public static final String PAYMENT_STATUS_APPROVED = "APPROVED";
    public static final String PAYMENT_STATUS_REJECTED = "REJECTED";

    // Resilience4j Instance Names
    public static final String RESILIENCE_CREDIT_CARD_SERVICE = "creditCardService";

    // Date/Time Patterns
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
