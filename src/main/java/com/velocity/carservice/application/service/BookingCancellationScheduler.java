package com.velocity.carservice.application.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.repository.BookingRepository;
import com.velocity.carservice.domain.service.BookingDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationScheduler {

    private final BookingRepository bookingRepository;
    private final BookingDomainService bookingDomainService;

    @Scheduled(fixedRateString = "${app.scheduler.cancellation-check-interval:3600000}")
    @SchedulerLock(
            name = "cancelUnpaidBankTransferBookings",
            lockAtLeastFor = "PT5M",
            lockAtMostFor = "PT30M"
    )
    @Transactional
    public void cancelUnpaidBankTransferBookings() {
        log.info("Running automatic cancellation check for unpaid bank transfer bookings");

        // Find bookings where rental starts within 48 hours (2 days)
        LocalDate cancellationDeadline = LocalDate.now().plusDays(2);

        List<Booking> pendingBookings = bookingRepository.findByPaymentModeAndStatus(
                PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT);

        int cancelledCount = 0;
        for (Booking booking : pendingBookings) {
            try {
                if (bookingDomainService.shouldAutoCancelBooking(booking)) {
                    cancelBooking(booking);
                    cancelledCount++;
                }
            } catch (Exception e) {
                log.error("Failed to cancel booking {}: {}", booking.getBookingId(), e.getMessage(), e);
            }
        }

        if (cancelledCount > 0) {
            log.info("Auto-cancelled {} unpaid bank transfer bookings", cancelledCount);
        } else {
            log.debug("No bookings to auto-cancel");
        }
    }

    private void cancelBooking(Booking booking) {
        log.info("Auto-cancelling booking {} - payment not received 48 hours before rental start ({})",
                booking.getBookingId(), booking.getRentalStartDate());

        booking.cancel();
        bookingRepository.save(booking);

        log.info("Booking {} cancelled due to unpaid bank transfer", booking.getBookingId());
    }
}
