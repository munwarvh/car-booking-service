package com.velocity.carservice.shared.constant;

public final class AppConstants {

    private AppConstants() {
    }

    // API Versioning
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;

    // Booking Constants
    public static final int BOOKING_REFERENCE_LENGTH = 16;
    public static final String BOOKING_REFERENCE_PREFIX = "VEL";
    public static final int MAX_BOOKINGS_PER_CUSTOMER = 10;

    // Payment Constants
    public static final int CREDIT_CARD_PAYMENT_TIMEOUT_MINUTES = 15;
    public static final int BANK_TRANSFER_PAYMENT_TIMEOUT_HOURS = 24;
    public static final String DEFAULT_CURRENCY = "EUR";

    // Kafka Topics
    public static final String TOPIC_PAYMENT_EVENTS = "payment-events";
    public static final String TOPIC_PAYMENT_CONFIRMATION = "payment-confirmation";
    public static final String TOPIC_BOOKING_EVENTS = "booking-events";

    // Cache Keys
    public static final String CACHE_BOOKINGS = "bookings";
    public static final String CACHE_CUSTOMERS = "customers";
    public static final String CACHE_VEHICLES = "vehicles";

    // HTTP Headers
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";

    // Error Codes
    public static final String ERROR_BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
    public static final String ERROR_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ERROR_INVALID_STATE = "INVALID_STATE";
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";

    // Scheduler Constants
    public static final long CANCELLATION_CHECK_INTERVAL_MS = 60000; // 1 minute
    public static final String CLEANUP_CRON = "0 0 2 * * ?"; // 2 AM daily

    // Vehicle Categories
    public static final String CATEGORY_ECONOMY = "ECONOMY";
    public static final String CATEGORY_COMPACT = "COMPACT";
    public static final String CATEGORY_MIDSIZE = "MIDSIZE";
    public static final String CATEGORY_FULLSIZE = "FULLSIZE";
    public static final String CATEGORY_SUV = "SUV";
    public static final String CATEGORY_LUXURY = "LUXURY";
}
