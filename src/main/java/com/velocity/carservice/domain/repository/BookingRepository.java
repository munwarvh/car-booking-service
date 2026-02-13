package com.velocity.carservice.domain.repository;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByPaymentModeAndStatus(PaymentMode paymentMode, BookingStatus status);

    List<Booking> findPendingBankTransferBookingsBeforeDate(LocalDate date);

    boolean existsByBookingId(String bookingId);

    void deleteById(UUID id);
}
