package com.velocity.carservice.domain.service;

import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.BookingValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BookingDomainService Unit Tests")
class BookingDomainServiceTest {

    private BookingDomainService bookingDomainService;

    @BeforeEach
    void setUp() {
        bookingDomainService = new BookingDomainService();
    }

    @Nested
    @DisplayName("Booking ID Generation Tests")
    class BookingIdGenerationTests {

        @Test
        @DisplayName("Should generate booking ID with correct format")
        void shouldGenerateBookingIdWithCorrectFormat() {
            // Act
            String bookingId = bookingDomainService.generateBookingId();

            // Assert
            assertThat(bookingId).startsWith("BKG");
            assertThat(bookingId).hasSize(10);
        }

        @Test
        @DisplayName("Should generate unique booking IDs")
        void shouldGenerateUniqueBookingIds() {
            // Act
            String bookingId1 = bookingDomainService.generateBookingId();
            String bookingId2 = bookingDomainService.generateBookingId();

            // Assert
            assertThat(bookingId1).isNotEqualTo(bookingId2);
        }
    }

    @Nested
    @DisplayName("Rental Date Validation Tests")
    class RentalDateValidationTests {

        @Test
        @DisplayName("Should accept valid rental dates")
        void shouldAcceptValidRentalDates() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(10);

            // Act & Assert - no exception should be thrown
            bookingDomainService.validateRentalDates(startDate, endDate);
        }

        @Test
        @DisplayName("Should reject when end date is before start date")
        void shouldRejectWhenEndDateBeforeStartDate() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(5);

            // Act & Assert
            assertThatThrownBy(() -> bookingDomainService.validateRentalDates(startDate, endDate))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Rental end date must be after rental start date");
        }

        @Test
        @DisplayName("Should reject when end date equals start date")
        void shouldRejectWhenEndDateEqualsStartDate() {
            // Arrange
            LocalDate sameDate = LocalDate.now().plusDays(5);

            // Act & Assert
            assertThatThrownBy(() -> bookingDomainService.validateRentalDates(sameDate, sameDate))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Rental end date must be after rental start date");
        }

        @Test
        @DisplayName("Should reject rental period exceeding 21 days")
        void shouldRejectRentalPeriodExceeding21Days() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(25); // 24 days rental

            // Act & Assert
            assertThatThrownBy(() -> bookingDomainService.validateRentalDates(startDate, endDate))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("cannot be booked for more than 21 days");
        }

        @Test
        @DisplayName("Should accept exactly 21 days rental")
        void shouldAcceptExactly21DaysRental() {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(22); // Exactly 21 days

            // Act & Assert - no exception should be thrown
            bookingDomainService.validateRentalDates(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Vehicle ID Validation Tests")
    class VehicleIdValidationTests {

        @Test
        @DisplayName("Should accept valid vehicle ID")
        void shouldAcceptValidVehicleId() {
            // Act & Assert - no exception should be thrown
            bookingDomainService.validateVehicleId("VH-12345");
        }

        @Test
        @DisplayName("Should reject null vehicle ID")
        void shouldRejectNullVehicleId() {
            // Act & Assert
            assertThatThrownBy(() -> bookingDomainService.validateVehicleId(null))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Vehicle ID is required");
        }

        @Test
        @DisplayName("Should reject blank vehicle ID")
        void shouldRejectBlankVehicleId() {
            // Act & Assert
            assertThatThrownBy(() -> bookingDomainService.validateVehicleId("   "))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Vehicle ID is required");
        }
    }

    @Nested
    @DisplayName("Auto Cancellation Logic Tests")
    class AutoCancellationTests {

        @Test
        @DisplayName("Should auto-cancel unpaid bank transfer booking within 48 hours of rental start")
        void shouldAutoCancelUnpaidBankTransferBooking() {
            // Arrange - booking starts tomorrow (within 48 hours)
            Booking booking = Booking.builder()
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .rentalStartDate(LocalDate.now().plusDays(1))
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .build();

            // Act
            boolean shouldCancel = bookingDomainService.shouldAutoCancelBooking(booking);

            // Assert
            assertThat(shouldCancel).isTrue();
        }

        @Test
        @DisplayName("Should not auto-cancel bank transfer booking with full payment")
        void shouldNotAutoCancelPaidBooking() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .rentalStartDate(LocalDate.now().plusDays(1))
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(new BigDecimal("200.00"))
                    .build();

            // Act
            boolean shouldCancel = bookingDomainService.shouldAutoCancelBooking(booking);

            // Assert
            assertThat(shouldCancel).isFalse();
        }

        @Test
        @DisplayName("Should not auto-cancel booking more than 48 hours before rental start")
        void shouldNotAutoCancelBookingMoreThan48HoursBefore() {
            // Arrange - booking starts in 5 days
            Booking booking = Booking.builder()
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .rentalStartDate(LocalDate.now().plusDays(5))
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .build();

            // Act
            boolean shouldCancel = bookingDomainService.shouldAutoCancelBooking(booking);

            // Assert
            assertThat(shouldCancel).isFalse();
        }

        @Test
        @DisplayName("Should not auto-cancel non-bank-transfer bookings")
        void shouldNotAutoCancelNonBankTransferBooking() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentMode(PaymentMode.CREDIT_CARD)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .rentalStartDate(LocalDate.now().plusDays(1))
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .build();

            // Act
            boolean shouldCancel = bookingDomainService.shouldAutoCancelBooking(booking);

            // Assert
            assertThat(shouldCancel).isFalse();
        }

        @Test
        @DisplayName("Should not auto-cancel already confirmed bookings")
        void shouldNotAutoCancelConfirmedBooking() {
            // Arrange
            Booking booking = Booking.builder()
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .status(BookingStatus.CONFIRMED)
                    .rentalStartDate(LocalDate.now().plusDays(1))
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(new BigDecimal("200.00"))
                    .build();

            // Act
            boolean shouldCancel = bookingDomainService.shouldAutoCancelBooking(booking);

            // Assert
            assertThat(shouldCancel).isFalse();
        }
    }

    @Nested
    @DisplayName("Initial Status Determination Tests")
    class InitialStatusTests {

        @Test
        @DisplayName("Should return CONFIRMED for digital wallet")
        void shouldReturnConfirmedForDigitalWallet() {
            // Act
            BookingStatus status = bookingDomainService.determineInitialStatus(PaymentMode.DIGITAL_WALLET);

            // Assert
            assertThat(status).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should return PENDING_PAYMENT for bank transfer")
        void shouldReturnPendingForBankTransfer() {
            // Act
            BookingStatus status = bookingDomainService.determineInitialStatus(PaymentMode.BANK_TRANSFER);

            // Assert
            assertThat(status).isEqualTo(BookingStatus.PENDING_PAYMENT);
        }
    }
}

