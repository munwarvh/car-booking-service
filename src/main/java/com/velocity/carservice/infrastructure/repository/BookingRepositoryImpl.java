package com.velocity.carservice.infrastructure.repository;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final JpaBookingRepository jpaBookingRepository;

    @Override
    public Booking save(Booking booking) {
        return jpaBookingRepository.save(booking);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jpaBookingRepository.findById(id);
    }

    @Override
    public Optional<Booking> findByBookingId(String bookingId) {
        return jpaBookingRepository.findByBookingId(bookingId);
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        return jpaBookingRepository.findByStatus(status);
    }

    @Override
    public List<Booking> findByPaymentModeAndStatus(PaymentMode paymentMode, BookingStatus status) {
        return jpaBookingRepository.findByPaymentModeAndStatus(paymentMode, status);
    }

    @Override
    public List<Booking> findPendingBankTransferBookingsBeforeDate(LocalDate date) {
        return jpaBookingRepository.findPendingBankTransferBookingsBeforeDate(date);
    }

    @Override
    public boolean existsByBookingId(String bookingId) {
        return jpaBookingRepository.existsByBookingId(bookingId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaBookingRepository.deleteById(id);
    }
}
