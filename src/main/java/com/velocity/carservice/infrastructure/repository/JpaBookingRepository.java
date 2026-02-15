package com.velocity.carservice.infrastructure.repository;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Batch update status for multiple bookings in a single query.
     * Much more efficient than updating one by one.
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus, b.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE b.bookingId IN :bookingIds")
    int batchUpdateStatus(@Param("bookingIds") List<String> bookingIds,
                          @Param("newStatus") BookingStatus newStatus);

    /**
     * Find booking IDs for auto-cancellation.
     * Returns only IDs (lightweight) for bookings that:
     * - Are BANK_TRANSFER payments
     * - Have PENDING_PAYMENT status
     * - Rental starts within specified days
     * - Payment not fully received
     */
    @Query("SELECT b.bookingId FROM Booking b " +
           "WHERE b.paymentMode = 'BANK_TRANSFER' " +
           "AND b.status = 'PENDING_PAYMENT' " +
           "AND b.rentalStartDate <= :deadline " +
           "AND (b.amountReceived IS NULL OR b.amountReceived < b.paymentAmount)")
    List<String> findBookingIdsForAutoCancellation(@Param("deadline") LocalDate deadline);
}
