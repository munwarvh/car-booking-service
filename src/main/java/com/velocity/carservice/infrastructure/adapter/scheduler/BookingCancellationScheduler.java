package com.velocity.carservice.infrastructure.adapter.scheduler;

import com.velocity.carservice.application.service.BookingCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for booking cancellation tasks.
 * This is an infrastructure component that handles scheduling concerns only.
 * Business logic is delegated to BookingCancellationService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationScheduler {

    private final BookingCancellationService bookingCancellationService;

    /**
     * Scheduled task to cancel unpaid bank transfer bookings.
     * Runs at a configurable interval and uses ShedLock for distributed locking.
     */
    @Scheduled(fixedRateString = "${app.scheduler.cancellation-check-interval:3600000}")
    @SchedulerLock(
            name = "cancelUnpaidBankTransferBookings",
            lockAtLeastFor = "PT5M",
            lockAtMostFor = "PT30M"
    )
    public void scheduleCancellationCheck() {
        log.info("Running scheduled cancellation check for unpaid bank transfer bookings");

        int cancelledCount = bookingCancellationService.cancelUnpaidBankTransferBookings();

        if (cancelledCount > 0) {
            log.info("Scheduled task completed: auto-cancelled {} unpaid bookings", cancelledCount);
        } else {
            log.debug("Scheduled task completed: no bookings to auto-cancel");
        }
    }
}

