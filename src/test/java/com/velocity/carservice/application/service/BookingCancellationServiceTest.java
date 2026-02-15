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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingCancellationService Unit Tests")
class BookingCancellationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDomainService bookingDomainService;

    @InjectMocks
    private BookingCancellationService cancellationService;

    @Test
    @DisplayName("Should batch cancel unpaid bank transfer bookings within 48 hours of rental start")
    void shouldBatchCancelUnpaidBookingsWithin48Hours() {
        // Arrange
        List<String> bookingIdsToCancel = Arrays.asList("BKG0000001", "BKG0000002");

        when(bookingRepository.findBookingIdsForAutoCancellation(anyInt()))
                .thenReturn(bookingIdsToCancel);
        when(bookingRepository.batchUpdateStatus(bookingIdsToCancel, BookingStatus.CANCELLED))
                .thenReturn(2);

        // Act
        int cancelledCount = cancellationService.cancelUnpaidBankTransferBookings();

        // Assert
        assertThat(cancelledCount).isEqualTo(2);
        verify(bookingRepository).findBookingIdsForAutoCancellation(2); // 2 days before rental
        verify(bookingRepository).batchUpdateStatus(bookingIdsToCancel, BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should not call batch update when no bookings found for cancellation")
    void shouldNotCallBatchUpdateWhenNoBookingsFound() {
        // Arrange
        when(bookingRepository.findBookingIdsForAutoCancellation(anyInt()))
                .thenReturn(Collections.emptyList());

        // Act
        int cancelledCount = cancellationService.cancelUnpaidBankTransferBookings();

        // Assert
        assertThat(cancelledCount).isEqualTo(0);
        verify(bookingRepository).findBookingIdsForAutoCancellation(2);
        verify(bookingRepository, never()).batchUpdateStatus(any(), any());
    }

    @Test
    @DisplayName("Should cancel single booking individually")
    void shouldCancelSingleBookingIndividually() {
        // Arrange
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000001")
                .customerName("Test User")
                .vehicleId("VH-001")
                .vehicleCategory(VehicleCategory.SEDAN)
                .rentalStartDate(LocalDate.now().plusDays(1))
                .rentalEndDate(LocalDate.now().plusDays(5))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-001")
                .paymentAmount(new BigDecimal("200.00"))
                .amountReceived(BigDecimal.ZERO)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cancellationService.cancelBooking(booking);

        // Assert
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should handle batch update with single booking")
    void shouldHandleBatchUpdateWithSingleBooking() {
        // Arrange
        List<String> singleBookingId = Collections.singletonList("BKG0000001");

        when(bookingRepository.findBookingIdsForAutoCancellation(anyInt()))
                .thenReturn(singleBookingId);
        when(bookingRepository.batchUpdateStatus(singleBookingId, BookingStatus.CANCELLED))
                .thenReturn(1);

        // Act
        int cancelledCount = cancellationService.cancelUnpaidBankTransferBookings();

        // Assert
        assertThat(cancelledCount).isEqualTo(1);
        verify(bookingRepository).batchUpdateStatus(singleBookingId, BookingStatus.CANCELLED);
    }
}
