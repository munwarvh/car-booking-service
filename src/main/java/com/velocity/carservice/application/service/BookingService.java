package com.velocity.carservice.application.service;

import com.velocity.carservice.application.dto.request.BookingRequestDTO;
import com.velocity.carservice.application.dto.response.BookingResponseDTO;
import com.velocity.carservice.application.strategy.PaymentStrategy;
import com.velocity.carservice.application.strategy.PaymentStrategyFactory;
import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.repository.BookingRepository;
import com.velocity.carservice.domain.service.BookingDomainService;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDomainService bookingDomainService;
    private final PaymentStrategyFactory paymentStrategyFactory;

    public BookingResponseDTO confirmBooking(BookingRequestDTO request) {
        log.info("Processing booking request for customer: {}, payment mode: {}",
                request.customerName(), request.paymentMode());

        bookingDomainService.validateRentalDates(request.rentalStartDate(), request.rentalEndDate());

        bookingDomainService.validateVehicleId(request.vehicleId());

        String bookingId = bookingDomainService.generateBookingId();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .customerName(request.customerName())
                .vehicleId(request.vehicleId())
                .vehicleCategory(request.vehicleCategory())
                .rentalStartDate(request.rentalStartDate())
                .rentalEndDate(request.rentalEndDate())
                .paymentMode(request.paymentMode())
                .paymentReference(request.paymentReference())
                .build();

        PaymentStrategy paymentStrategy = paymentStrategyFactory.getStrategy(request.paymentMode());
        BookingStatus status = paymentStrategy.processPayment(booking, request.paymentReference());
        booking.setStatus(status);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} created with status: {}", savedBooking.getBookingId(), savedBooking.getStatus());

        return new BookingResponseDTO(savedBooking.getBookingId(), savedBooking.getStatus());
    }

    /**
     * Processes bank transfer payment received via Kafka event
     */
    public void processBankTransferPayment(String bookingId, BigDecimal amountReceived) {
        log.info("Processing bank transfer payment for booking: {}, amount: {}", bookingId, amountReceived);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            log.warn("Booking {} is not in PENDING_PAYMENT status, current: {}", bookingId, booking.getStatus());
            return;
        }

        BigDecimal totalReceived = booking.getAmountReceived() != null
                ? booking.getAmountReceived().add(amountReceived)
                : amountReceived;
        booking.setAmountReceived(totalReceived);

        // Check if full payment received
        if (booking.isFullPaymentReceived()) {
            booking.confirm();
            log.info("Full payment received for booking {}. Status changed to CONFIRMED", bookingId);
        } else {
            log.info("Partial payment received for booking {}. Total received: {}, Required: {}",
                    bookingId, totalReceived, booking.getPaymentAmount());
        }

        bookingRepository.save(booking);
    }

    /**
     * Get booking by booking ID
     */
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(String bookingId) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
        return new BookingResponseDTO(booking.getBookingId(), booking.getStatus());
    }

    /**
     * Cancel a booking
     */
    public BookingResponseDTO cancelBooking(String bookingId) {
        log.info("Cancelling booking: {}", bookingId);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        bookingDomainService.validateBookingForCancellation(booking);

        booking.cancel();
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingId);
        return new BookingResponseDTO(savedBooking.getBookingId(), savedBooking.getStatus());
    }
}
