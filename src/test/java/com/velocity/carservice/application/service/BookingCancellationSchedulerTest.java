package com.velocity.carservice.application.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import com.velocity.carservice.domain.repository.BookingRepository;
import com.velocity.carservice.domain.service.BookingDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingCancellationScheduler Unit Tests")
class BookingCancellationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDomainService bookingDomainService;

    @InjectMocks
    private BookingCancellationScheduler cancellationScheduler;

    private Booking unpaidBookingWithin48Hours;
    private Booking unpaidBookingMoreThan48Hours;
    private Booking paidBooking;

    @BeforeEach
    void setUp() {
        // Booking that should be cancelled (within 48 hours, unpaid)
        unpaidBookingWithin48Hours = Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000001")
                .customerName("Test User 1")
                .vehicleId("VH-001")
                .vehicleCategory(VehicleCategory.SEDAN)
                .rentalStartDate(LocalDate.now().plusDays(1)) // Tomorrow - within 48 hours
                .rentalEndDate(LocalDate.now().plusDays(5))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-001")
                .paymentAmount(new BigDecimal("200.00"))
                .amountReceived(BigDecimal.ZERO) // Not paid
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        // Booking that should NOT be cancelled (more than 48 hours away)
        unpaidBookingMoreThan48Hours = Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000002")
                .customerName("Test User 2")
                .vehicleId("VH-002")
                .vehicleCategory(VehicleCategory.SUV)
                .rentalStartDate(LocalDate.now().plusDays(5)) // 5 days away
                .rentalEndDate(LocalDate.now().plusDays(10))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-002")
                .paymentAmount(new BigDecimal("300.00"))
                .amountReceived(BigDecimal.ZERO) // Not paid
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        // Booking that should NOT be cancelled (fully paid)
        paidBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000003")
                .customerName("Test User 3")
                .vehicleId("VH-003")
                .vehicleCategory(VehicleCategory.COMPACT)
                .rentalStartDate(LocalDate.now().plusDays(1)) // Tomorrow
                .rentalEndDate(LocalDate.now().plusDays(3))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-003")
                .paymentAmount(new BigDecimal("150.00"))
                .amountReceived(new BigDecimal("150.00")) // Fully paid
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
    }

    @Test
    @DisplayName("Should cancel unpaid bank transfer bookings within 48 hours of rental start")
    void shouldCancelUnpaidBookingsWithin48Hours() {
        // Arrange
        when(bookingRepository.findByPaymentModeAndStatus(PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT))
                .thenReturn(Arrays.asList(unpaidBookingWithin48Hours, unpaidBookingMoreThan48Hours, paidBooking));

        when(bookingDomainService.shouldAutoCancelBooking(unpaidBookingWithin48Hours)).thenReturn(true);
        when(bookingDomainService.shouldAutoCancelBooking(unpaidBookingMoreThan48Hours)).thenReturn(false);
        when(bookingDomainService.shouldAutoCancelBooking(paidBooking)).thenReturn(false);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cancellationScheduler.cancelUnpaidBankTransferBookings();

        // Assert
        verify(bookingRepository, times(1)).save(unpaidBookingWithin48Hours);
        verify(bookingRepository, never()).save(unpaidBookingMoreThan48Hours);
        verify(bookingRepository, never()).save(paidBooking);
    }

    @Test
    @DisplayName("Should not cancel any bookings when none are pending")
    void shouldNotCancelWhenNoPendingBookings() {
        // Arrange
        when(bookingRepository.findByPaymentModeAndStatus(PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT))
                .thenReturn(Collections.emptyList());

        // Act
        cancellationScheduler.cancelUnpaidBankTransferBookings();

        // Assert
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle exception during cancellation gracefully")
    void shouldHandleExceptionGracefully() {
        // Arrange
        when(bookingRepository.findByPaymentModeAndStatus(PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT))
                .thenReturn(Arrays.asList(unpaidBookingWithin48Hours));

        when(bookingDomainService.shouldAutoCancelBooking(unpaidBookingWithin48Hours)).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        cancellationScheduler.cancelUnpaidBankTransferBookings();

        // Assert - method completed without throwing
        verify(bookingRepository).save(unpaidBookingWithin48Hours);
    }

    @Test
    @DisplayName("Should cancel multiple unpaid bookings in single run")
    void shouldCancelMultipleUnpaidBookings() {
        // Arrange
        Booking anotherUnpaidBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000004")
                .customerName("Test User 4")
                .vehicleId("VH-004")
                .vehicleCategory(VehicleCategory.LUXURY)
                .rentalStartDate(LocalDate.now().plusDays(1))
                .rentalEndDate(LocalDate.now().plusDays(4))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-004")
                .paymentAmount(new BigDecimal("500.00"))
                .amountReceived(BigDecimal.ZERO)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.findByPaymentModeAndStatus(PaymentMode.BANK_TRANSFER, BookingStatus.PENDING_PAYMENT))
                .thenReturn(Arrays.asList(unpaidBookingWithin48Hours, anotherUnpaidBooking));

        when(bookingDomainService.shouldAutoCancelBooking(any())).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cancellationScheduler.cancelUnpaidBankTransferBookings();

        // Assert
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }
}

