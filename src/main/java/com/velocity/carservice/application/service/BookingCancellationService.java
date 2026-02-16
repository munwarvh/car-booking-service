package com.velocity.carservice.application.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {

    private static final int DAYS_BEFORE_RENTAL_FOR_CANCELLATION = 2;

    private final BookingRepository bookingRepository;

    @Transactional
    public int cancelUnpaidBankTransferBookings() {
        log.info("Checking for unpaid bank transfer bookings to cancel (batch mode)");

        List<String> bookingIdsToCancel = bookingRepository
                .findBookingIdsForAutoCancellation(DAYS_BEFORE_RENTAL_FOR_CANCELLATION);

        if (bookingIdsToCancel.isEmpty()) {
            log.debug("No bookings found for auto-cancellation");
            return 0;
        }

        log.info("Found {} bookings to auto-cancel: {}", bookingIdsToCancel.size(), bookingIdsToCancel);

        int cancelledCount = bookingRepository.batchUpdateStatus(bookingIdsToCancel, BookingStatus.CANCELLED);

        log.info("Batch cancellation completed. Cancelled {} bookings", cancelledCount);
        return cancelledCount;
    }

    @Transactional
    public void cancelBooking(Booking booking) {
        log.info("Cancelling booking {} - payment not received 48 hours before rental start ({})",
                booking.getBookingId(), booking.getRentalStartDate());

        booking.cancel();
        bookingRepository.save(booking);

        log.info("Booking {} cancelled due to unpaid bank transfer", booking.getBookingId());
    }
}
