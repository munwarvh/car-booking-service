package com.velocity.carservice.infrastructure.metrics;

import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom metrics for car booking service observability.
 * Tracks booking operations, payment processing, and business KPIs.
 */
@Component
public class BookingMetrics {

    private static final String METRIC_PREFIX = "car_booking_";

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter bookingsCreatedTotal;
    private final Counter bookingsConfirmedTotal;
    private final Counter bookingsCancelledTotal;
    private final Counter bookingsAutoCancelledTotal;
    private final Counter paymentEventsReceivedTotal;
    private final Counter paymentEventsProcessedTotal;
    private final Counter paymentEventsFailedTotal;

    // Timers
    private final Timer bookingCreationTimer;
    private final Timer paymentValidationTimer;

    public BookingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Booking counters
        this.bookingsCreatedTotal = Counter.builder(METRIC_PREFIX + "bookings_created_total")
                .description("Total number of bookings created")
                .register(meterRegistry);

        this.bookingsConfirmedTotal = Counter.builder(METRIC_PREFIX + "bookings_confirmed_total")
                .description("Total number of bookings confirmed")
                .register(meterRegistry);

        this.bookingsCancelledTotal = Counter.builder(METRIC_PREFIX + "bookings_cancelled_total")
                .description("Total number of bookings cancelled by user")
                .register(meterRegistry);

        this.bookingsAutoCancelledTotal = Counter.builder(METRIC_PREFIX + "bookings_auto_cancelled_total")
                .description("Total number of bookings auto-cancelled due to non-payment")
                .register(meterRegistry);

        // Payment event counters
        this.paymentEventsReceivedTotal = Counter.builder(METRIC_PREFIX + "payment_events_received_total")
                .description("Total number of bank transfer payment events received")
                .register(meterRegistry);

        this.paymentEventsProcessedTotal = Counter.builder(METRIC_PREFIX + "payment_events_processed_total")
                .description("Total number of payment events successfully processed")
                .register(meterRegistry);

        this.paymentEventsFailedTotal = Counter.builder(METRIC_PREFIX + "payment_events_failed_total")
                .description("Total number of payment events that failed processing")
                .register(meterRegistry);

        // Timers
        this.bookingCreationTimer = Timer.builder(METRIC_PREFIX + "booking_creation_duration_seconds")
                .description("Time taken to create a booking")
                .register(meterRegistry);

        this.paymentValidationTimer = Timer.builder(METRIC_PREFIX + "payment_validation_duration_seconds")
                .description("Time taken to validate payment")
                .register(meterRegistry);
    }

    // ==================== Booking Metrics ====================

    public void incrementBookingsCreated(PaymentMode paymentMode, VehicleCategory category) {
        bookingsCreatedTotal.increment();
        Counter.builder(METRIC_PREFIX + "bookings_created_by_payment_mode")
                .tag("payment_mode", paymentMode.name())
                .tag("vehicle_category", category.name())
                .register(meterRegistry)
                .increment();
    }

    public void incrementBookingsConfirmed(PaymentMode paymentMode) {
        bookingsConfirmedTotal.increment();
        Counter.builder(METRIC_PREFIX + "bookings_confirmed_by_payment_mode")
                .tag("payment_mode", paymentMode.name())
                .register(meterRegistry)
                .increment();
    }

    public void incrementBookingsCancelled() {
        bookingsCancelledTotal.increment();
    }

    public void incrementBookingsAutoCancelled(int count) {
        bookingsAutoCancelledTotal.increment(count);
    }

    public void recordBookingCreationTime(long durationMs) {
        bookingCreationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== Payment Metrics ====================

    public void incrementPaymentEventsReceived() {
        paymentEventsReceivedTotal.increment();
    }

    public void incrementPaymentEventsProcessed() {
        paymentEventsProcessedTotal.increment();
    }

    public void incrementPaymentEventsFailed(String reason) {
        paymentEventsFailedTotal.increment();
        Counter.builder(METRIC_PREFIX + "payment_events_failed_by_reason")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentValidationTime(long durationMs) {
        paymentValidationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== Status Gauge ====================

    public void recordBookingStatusChange(BookingStatus oldStatus, BookingStatus newStatus) {
        if (newStatus == BookingStatus.CONFIRMED) {
            incrementBookingsConfirmed(PaymentMode.BANK_TRANSFER);
        }
    }
}

