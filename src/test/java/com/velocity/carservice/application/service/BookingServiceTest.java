package com.velocity.carservice.application.service;

import com.velocity.carservice.application.dto.request.BookingRequestDTO;
import com.velocity.carservice.application.dto.response.BookingResponseDTO;
import com.velocity.carservice.application.strategy.PaymentStrategy;
import com.velocity.carservice.application.strategy.PaymentStrategyFactory;
import com.velocity.carservice.domain.model.Booking;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import com.velocity.carservice.domain.repository.BookingRepository;
import com.velocity.carservice.domain.service.BookingDomainService;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.BookingNotFoundException;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.PaymentFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDomainService bookingDomainService;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new BookingRequestDTO(
                "John Doe",
                "VH-001",
                VehicleCategory.SEDAN,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10),
                PaymentMode.DIGITAL_WALLET,
                "PAY-REF-001",
                new BigDecimal("250.00")
        );
    }

    @Nested
    @DisplayName("Digital Wallet Payment Tests")
    class DigitalWalletPaymentTests {

        @Test
        @DisplayName("Should confirm booking immediately for digital wallet payment")
        void shouldConfirmBookingImmediatelyForDigitalWallet() {
            // Arrange
            when(bookingDomainService.generateBookingId()).thenReturn("BKG0000001");
            when(paymentStrategyFactory.getStrategy(PaymentMode.DIGITAL_WALLET)).thenReturn(paymentStrategy);
            when(paymentStrategy.processPayment(any(Booking.class), anyString())).thenReturn(BookingStatus.CONFIRMED);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponseDTO response = bookingService.confirmBooking(validRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.bookingId()).isEqualTo("BKG0000001");
            assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);

            verify(bookingDomainService).validateRentalDates(any(), any());
            verify(bookingDomainService).validateVehicleId(anyString());
            verify(paymentStrategyFactory).getStrategy(PaymentMode.DIGITAL_WALLET);
            verify(paymentStrategy).processPayment(any(Booking.class), eq("PAY-REF-001"));
        }
    }

    @Nested
    @DisplayName("Credit Card Payment Tests")
    class CreditCardPaymentTests {

        @Test
        @DisplayName("Should confirm booking when credit card is approved")
        void shouldConfirmBookingWhenCreditCardApproved() {
            // Arrange
            BookingRequestDTO creditCardRequest = new BookingRequestDTO(
                    "Jane Smith",
                    "VH-002",
                    VehicleCategory.SUV,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.CREDIT_CARD,
                    "CC-REF-001",
                    new BigDecimal("450.00")
            );

            when(bookingDomainService.generateBookingId()).thenReturn("BKG0000002");
            when(paymentStrategyFactory.getStrategy(PaymentMode.CREDIT_CARD)).thenReturn(paymentStrategy);
            when(paymentStrategy.processPayment(any(Booking.class), anyString())).thenReturn(BookingStatus.CONFIRMED);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponseDTO response = bookingService.confirmBooking(creditCardRequest);

            // Assert
            assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(paymentStrategyFactory).getStrategy(PaymentMode.CREDIT_CARD);
            verify(paymentStrategy).processPayment(any(Booking.class), eq("CC-REF-001"));
        }

        @Test
        @DisplayName("Should throw exception when credit card is rejected")
        void shouldThrowExceptionWhenCreditCardRejected() {
            // Arrange
            BookingRequestDTO creditCardRequest = new BookingRequestDTO(
                    "Bob Wilson",
                    "VH-003",
                    VehicleCategory.LUXURY,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.CREDIT_CARD,
                    "REJECT-001",
                    new BigDecimal("800.00")
            );

            when(bookingDomainService.generateBookingId()).thenReturn("BKG0000003");
            when(paymentStrategyFactory.getStrategy(PaymentMode.CREDIT_CARD)).thenReturn(paymentStrategy);
            when(paymentStrategy.processPayment(any(Booking.class), anyString()))
                    .thenThrow(new PaymentFailedException("Credit card payment was not approved"));

            // Act & Assert
            assertThatThrownBy(() -> bookingService.confirmBooking(creditCardRequest))
                    .isInstanceOf(PaymentFailedException.class)
                    .hasMessageContaining("Credit card payment was not approved");

            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Bank Transfer Payment Tests")
    class BankTransferPaymentTests {

        @Test
        @DisplayName("Should create booking with PENDING_PAYMENT status for bank transfer")
        void shouldCreatePendingBookingForBankTransfer() {
            // Arrange
            BookingRequestDTO bankTransferRequest = new BookingRequestDTO(
                    "Alice Brown",
                    "VH-004",
                    VehicleCategory.COMPACT,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.BANK_TRANSFER,
                    "BT-REF-001",
                    new BigDecimal("150.00")
            );

            when(bookingDomainService.generateBookingId()).thenReturn("BKG0000004");
            when(paymentStrategyFactory.getStrategy(PaymentMode.BANK_TRANSFER)).thenReturn(paymentStrategy);
            when(paymentStrategy.processPayment(any(Booking.class), anyString())).thenReturn(BookingStatus.PENDING_PAYMENT);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponseDTO response = bookingService.confirmBooking(bankTransferRequest);

            // Assert
            assertThat(response.bookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
            verify(paymentStrategyFactory).getStrategy(PaymentMode.BANK_TRANSFER);
            verify(paymentStrategy).processPayment(any(Booking.class), eq("BT-REF-001"));
        }

        @Test
        @DisplayName("Should confirm booking when full payment is received via bank transfer")
        void shouldConfirmBookingWhenFullPaymentReceived() {
            // Arrange
            Booking pendingBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .bookingId("BKG0000005")
                    .customerName("Test User")
                    .vehicleId("VH-005")
                    .vehicleCategory(VehicleCategory.SEDAN)
                    .rentalStartDate(LocalDate.now().plusDays(5))
                    .rentalEndDate(LocalDate.now().plusDays(10))
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .paymentReference("BT-REF-002")
                    .paymentAmount(new BigDecimal("200.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .build();

            when(bookingRepository.findByBookingId("BKG0000005")).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            bookingService.processBankTransferPayment("BKG0000005", new BigDecimal("200.00"));

            // Assert
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());

            Booking savedBooking = bookingCaptor.getValue();
            assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(savedBooking.getAmountReceived()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Should keep PENDING_PAYMENT status for partial payment")
        void shouldKeepPendingStatusForPartialPayment() {
            // Arrange
            Booking pendingBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .bookingId("BKG0000006")
                    .customerName("Test User")
                    .vehicleId("VH-006")
                    .vehicleCategory(VehicleCategory.SEDAN)
                    .rentalStartDate(LocalDate.now().plusDays(5))
                    .rentalEndDate(LocalDate.now().plusDays(10))
                    .paymentMode(PaymentMode.BANK_TRANSFER)
                    .paymentReference("BT-REF-003")
                    .paymentAmount(new BigDecimal("300.00"))
                    .amountReceived(BigDecimal.ZERO)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .build();

            when(bookingRepository.findByBookingId("BKG0000006")).thenReturn(Optional.of(pendingBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            bookingService.processBankTransferPayment("BKG0000006", new BigDecimal("100.00"));

            // Assert
            ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(bookingCaptor.capture());

            Booking savedBooking = bookingCaptor.getValue();
            assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
            assertThat(savedBooking.getAmountReceived()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("Get Booking Tests")
    class GetBookingTests {

        @Test
        @DisplayName("Should return booking when found")
        void shouldReturnBookingWhenFound() {
            // Arrange
            Booking booking = Booking.builder()
                    .bookingId("BKG0000007")
                    .status(BookingStatus.CONFIRMED)
                    .build();

            when(bookingRepository.findByBookingId("BKG0000007")).thenReturn(Optional.of(booking));

            // Act
            BookingResponseDTO response = bookingService.getBookingById("BKG0000007");

            // Assert
            assertThat(response.bookingId()).isEqualTo("BKG0000007");
            assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            when(bookingRepository.findByBookingId("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookingService.getBookingById("INVALID"))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void shouldCancelBookingSuccessfully() {
            // Arrange
            Booking booking = Booking.builder()
                    .bookingId("BKG0000008")
                    .status(BookingStatus.PENDING_PAYMENT)
                    .build();

            when(bookingRepository.findByBookingId("BKG0000008")).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BookingResponseDTO response = bookingService.cancelBooking("BKG0000008");

            // Assert
            assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingDomainService).validateBookingForCancellation(booking);
        }
    }
}
