package com.velocity.carservice.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Booking Entity Unit Tests")
class BookingTest {

    @Nested
    @DisplayName("Status Change Tests")
    class StatusChangeTests {

        @Test
        @DisplayName("Should confirm booking")
        void shouldConfirmBooking() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.PENDING_PAYMENT);

            // Act
            booking.confirm();

            // Assert
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should cancel booking")
        void shouldCancelBooking() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.PENDING_PAYMENT);

            // Act
            booking.cancel();

            // Assert
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should set pending payment status")
        void shouldSetPendingPaymentStatus() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.CONFIRMED);

            // Act
            booking.setPendingPayment();

            // Assert
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        }
    }

    @Nested
    @DisplayName("Cancellation Eligibility Tests")
    class CancellationEligibilityTests {

        @Test
        @DisplayName("Should allow cancellation for pending payment booking")
        void shouldAllowCancellationForPendingPayment() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.PENDING_PAYMENT);

            // Act & Assert
            assertThat(booking.canBeCancelled()).isTrue();
        }

        @Test
        @DisplayName("Should not allow cancellation for confirmed booking")
        void shouldNotAllowCancellationForConfirmed() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.CONFIRMED);

            // Act & Assert
            assertThat(booking.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("Should not allow cancellation for already cancelled booking")
        void shouldNotAllowCancellationForCancelled() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.CANCELLED);

            // Act & Assert
            assertThat(booking.canBeCancelled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Pending Payment Check Tests")
    class PendingPaymentCheckTests {

        @Test
        @DisplayName("Should return true for pending payment status")
        void shouldReturnTrueForPendingPayment() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.PENDING_PAYMENT);

            // Act & Assert
            assertThat(booking.isPendingPayment()).isTrue();
        }

        @Test
        @DisplayName("Should return false for confirmed status")
        void shouldReturnFalseForConfirmed() {
            // Arrange
            Booking booking = createTestBooking(BookingStatus.CONFIRMED);

            // Act & Assert
            assertThat(booking.isPendingPayment()).isFalse();
        }
    }

    @Nested
    @DisplayName("Full Payment Check Tests")
    class FullPaymentCheckTests {

        @Test
        @DisplayName("Should return true when full payment received")
        void shouldReturnTrueWhenFullPaymentReceived() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(new BigDecimal("200.00"))
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isTrue();
        }

        @Test
        @DisplayName("Should return true when overpayment received")
        void shouldReturnTrueWhenOverpaymentReceived() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(new BigDecimal("250.00"))
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isTrue();
        }

        @Test
        @DisplayName("Should return false when partial payment received")
        void shouldReturnFalseWhenPartialPaymentReceived() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(new BigDecimal("100.00"))
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isFalse();
        }

        @Test
        @DisplayName("Should return false when no payment received")
        void shouldReturnFalseWhenNoPaymentReceived() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isFalse();
        }

        @Test
        @DisplayName("Should return false when amount received is null")
        void shouldReturnFalseWhenAmountReceivedIsNull() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(null)
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isFalse();
        }

        @Test
        @DisplayName("Should return false when payment amount is null")
        void shouldReturnFalseWhenPaymentAmountIsNull() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentAmount(null)
                    .amountReceived(new BigDecimal("100.00"))
                    .build();

            // Act & Assert
            assertThat(booking.isFullPaymentReceived()).isFalse();
        }
    }

    private Booking createTestBooking(BookingStatus status) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingId("BKG0000001")
                .customerName("Test User")
                .vehicleId("VH-001")
                .vehicleCategory(VehicleCategory.SEDAN)
                .rentalStartDate(LocalDate.now().plusDays(5))
                .rentalEndDate(LocalDate.now().plusDays(10))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .paymentReference("BT-001")
                .paymentAmount(new BigDecimal("200.00"))
                .amountReceived(BigDecimal.ZERO)
                .status(status)
                .build();
    }
}

