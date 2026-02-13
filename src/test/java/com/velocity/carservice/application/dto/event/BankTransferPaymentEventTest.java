package com.velocity.carservice.application.dto.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BankTransferPaymentEvent Tests")
class BankTransferPaymentEventTest {

    @Nested
    @DisplayName("Booking ID Extraction Tests")
    class BookingIdExtractionTests {

        @Test
        @DisplayName("Should extract booking ID from valid transaction details")
        void shouldExtractBookingIdFromValidTransactionDetails() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-001",
                    "NL91ABNA0417164300",
                    new BigDecimal("200.00"),
                    "TXN987654321 BKG0012345"
            );

            // Act
            String bookingId = event.extractBookingId();

            // Assert
            assertThat(bookingId).isEqualTo("BKG0012345");
        }

        @Test
        @DisplayName("Should return null for null transaction details")
        void shouldReturnNullForNullTransactionDetails() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-002",
                    "NL91ABNA0417164300",
                    new BigDecimal("100.00"),
                    null
            );

            // Act
            String bookingId = event.extractBookingId();

            // Assert
            assertThat(bookingId).isNull();
        }

        @Test
        @DisplayName("Should return null for transaction details shorter than 23 characters")
        void shouldReturnNullForShortTransactionDetails() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-003",
                    "NL91ABNA0417164300",
                    new BigDecimal("150.00"),
                    "SHORT"
            );

            // Act
            String bookingId = event.extractBookingId();

            // Assert
            assertThat(bookingId).isNull();
        }

        @Test
        @DisplayName("Should trim booking ID")
        void shouldTrimBookingId() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-004",
                    "NL91ABNA0417164300",
                    new BigDecimal("300.00"),
                    "TXN987654321 BKG0012345   "
            );

            // Act
            String bookingId = event.extractBookingId();

            // Assert
            assertThat(bookingId).isEqualTo("BKG0012345");
        }
    }

    @Nested
    @DisplayName("Transaction Reference Extraction Tests")
    class TransactionReferenceExtractionTests {

        @Test
        @DisplayName("Should extract transaction reference from valid transaction details")
        void shouldExtractTransactionReference() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-005",
                    "NL91ABNA0417164300",
                    new BigDecimal("500.00"),
                    "TXN987654321 BKG0012345"
            );

            // Act
            String txnRef = event.extractTransactionReference();

            // Assert
            assertThat(txnRef).isEqualTo("TXN987654321");
        }

        @Test
        @DisplayName("Should return null for null transaction details")
        void shouldReturnNullForNullTransactionDetailsWhenExtractingTxnRef() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-006",
                    "NL91ABNA0417164300",
                    new BigDecimal("250.00"),
                    null
            );

            // Act
            String txnRef = event.extractTransactionReference();

            // Assert
            assertThat(txnRef).isNull();
        }

        @Test
        @DisplayName("Should return null for transaction details shorter than 12 characters")
        void shouldReturnNullForShortTransactionDetailsWhenExtractingTxnRef() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-007",
                    "NL91ABNA0417164300",
                    new BigDecimal("175.00"),
                    "SHORT"
            );

            // Act
            String txnRef = event.extractTransactionReference();

            // Assert
            assertThat(txnRef).isNull();
        }
    }

    @Nested
    @DisplayName("Record Accessor Tests")
    class RecordAccessorTests {

        @Test
        @DisplayName("Should return correct payment ID")
        void shouldReturnCorrectPaymentId() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-008",
                    "NL91ABNA0417164300",
                    new BigDecimal("400.00"),
                    "TXN987654321 BKG0012345"
            );

            // Act & Assert
            assertThat(event.paymentId()).isEqualTo("PAY-008");
        }

        @Test
        @DisplayName("Should return correct sender account number")
        void shouldReturnCorrectSenderAccountNumber() {
            // Arrange
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-009",
                    "DE89370400440532013000",
                    new BigDecimal("600.00"),
                    "TXN987654321 BKG0012345"
            );

            // Act & Assert
            assertThat(event.senderAccountNumber()).isEqualTo("DE89370400440532013000");
        }

        @Test
        @DisplayName("Should return correct payment amount")
        void shouldReturnCorrectPaymentAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("750.50");
            BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                    "PAY-010",
                    "NL91ABNA0417164300",
                    amount,
                    "TXN987654321 BKG0012345"
            );

            // Act & Assert
            assertThat(event.paymentAmount()).isEqualByComparingTo(amount);
        }
    }
}

