package com.velocity.carservice.application.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.repository.BookingRepository;
import com.velocity.carservice.domain.service.BookingDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for booking cancellation operations.
 * Contains the business logic for cancelling bookings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {

    private static final int DAYS_BEFORE_RENTAL_FOR_CANCELLATION = 2;

    private final BookingRepository bookingRepository;
    private final BookingDomainService bookingDomainService;

    /**
     * Cancels all unpaid bank transfer bookings that are due for cancellation.
     * Uses batch update for better performance - single DB call instead of N calls.
     * A booking is cancelled if payment is not received 48 hours before rental start.
     *
     * @return the number of bookings cancelled
     */
    @Transactional
    public int cancelUnpaidBankTransferBookings() {
        log.info("Checking for unpaid bank transfer bookings to cancel (batch mode)");

        // Find all booking IDs that need cancellation - lightweight query returning only IDs
        List<String> bookingIdsToCancel = bookingRepository
                .findBookingIdsForAutoCancellation(DAYS_BEFORE_RENTAL_FOR_CANCELLATION);

        if (bookingIdsToCancel.isEmpty()) {
            log.debug("No bookings found for auto-cancellation");
            return 0;
        }

        log.info("Found {} bookings to auto-cancel: {}", bookingIdsToCancel.size(), bookingIdsToCancel);

        // Batch update - single SQL UPDATE statement
        int cancelledCount = bookingRepository.batchUpdateStatus(bookingIdsToCancel, BookingStatus.CANCELLED);

        log.info("Batch cancellation completed. Cancelled {} bookings", cancelledCount);
        return cancelledCount;
    }

    /**
     * Cancel a specific booking due to non-payment.
     * Used for individual cancellation (e.g., manual cancellation).
     *
     * @param booking the booking to cancel
     */
    @Transactional
    public void cancelBooking(Booking booking) {
        log.info("Cancelling booking {} - payment not received 48 hours before rental start ({})",
                booking.getBookingId(), booking.getRentalStartDate());

        booking.cancel();
        bookingRepository.save(booking);

        log.info("Booking {} cancelled due to unpaid bank transfer", booking.getBookingId());
    }
}
