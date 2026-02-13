package com.velocity.carservice.infrastructure.repository;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByPaymentModeAndStatus(PaymentMode paymentMode, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.paymentMode = 'BANK_TRANSFER' " +
           "AND b.status = 'PENDING_PAYMENT' " +
           "AND b.rentalStartDate <= :deadline")
    List<Booking> findPendingBankTransferBookingsBeforeDate(@Param("deadline") LocalDate deadline);

    boolean existsByBookingId(String bookingId);
}
