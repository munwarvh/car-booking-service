package com.velocity.carservice.domain.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.BookingValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class BookingDomainService {

    private static final int MAX_RENTAL_DAYS = 21;
    private static final AtomicLong bookingSequence = new AtomicLong(1);

    /**
     * Generates a unique 10-character booking ID
     * Format: BKG followed by 7-digit sequential number (e.g., BKG0012345)
     */
    public String generateBookingId() {
        long sequence = bookingSequence.getAndIncrement();
        return String.format("BKG%07d", sequence % 10000000);
    }

    /**
     * Validates rental dates according to business rules
     * - End date must be after start date
     * - Maximum rental period is 21 days
     */
    public void validateRentalDates(LocalDate rentalStartDate, LocalDate rentalEndDate) {
        if (rentalEndDate.isBefore(rentalStartDate) || rentalEndDate.isEqual(rentalStartDate)) {
            throw new BookingValidationException("Rental end date must be after rental start date");
        }

        long rentalDays = ChronoUnit.DAYS.between(rentalStartDate, rentalEndDate);
        if (rentalDays > MAX_RENTAL_DAYS) {
            throw new BookingValidationException(
                    "A vehicle cannot be booked for more than " + MAX_RENTAL_DAYS + " days. Requested: " + rentalDays + " days");
        }
    }

    /**
     * Validates vehicle ID (mock validation)
     */
    public void validateVehicleId(String vehicleId) {
        if (vehicleId == null || vehicleId.isBlank()) {
            throw new BookingValidationException("Vehicle ID is required");
        }
        log.debug("Vehicle ID {} validated successfully", vehicleId);
    }

    /**
     * Determines initial booking status based on payment mode
     * - CASH (digital wallet) -> CONFIRMED immediately
     * - CREDIT_CARD -> depends on validation result (handled in service)
     * - BANK_TRANSFER -> PENDING_PAYMENT
     */
    public BookingStatus determineInitialStatus(PaymentMode paymentMode) {
        return switch (paymentMode) {
            case DIGITAL_WALLET -> BookingStatus.CONFIRMED;
            case CREDIT_CARD -> BookingStatus.CONFIRMED;
            case BANK_TRANSFER -> BookingStatus.PENDING_PAYMENT;
        };
    }

    /**
     * Checks if a booking should be auto-cancelled
     * Bank transfer bookings must be fully paid 48 hours before rental start
     */
    public boolean shouldAutoCancelBooking(Booking booking) {
        if (booking.getPaymentMode() != PaymentMode.BANK_TRANSFER) {
            return false;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return false;
        }
        if (booking.isFullPaymentReceived()) {
            return false;
        }

        LocalDate cancellationDeadline = booking.getRentalStartDate().minusDays(2);
        return !LocalDate.now().isBefore(cancellationDeadline);
    }

    public void validateBookingForCancellation(Booking booking) {
        if (!booking.canBeCancelled()) {
            throw new IllegalStateException(
                    "Booking cannot be cancelled. Current status: " + booking.getStatus());
        }
    }
}
