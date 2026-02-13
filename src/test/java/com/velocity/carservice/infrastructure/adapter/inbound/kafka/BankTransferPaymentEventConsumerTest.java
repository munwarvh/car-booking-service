package com.velocity.carservice.infrastructure.adapter.inbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.event.BankTransferPaymentEvent;
import com.velocity.carservice.application.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankTransferPaymentEventConsumer Unit Tests")
class BankTransferPaymentEventConsumerTest {

    @Mock
    private BookingService bookingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private BankTransferPaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Should process valid bank transfer payment event")
    void shouldProcessValidPaymentEvent() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-001",
                "NL91ABNA0417164300",
                new BigDecimal("200.00"),
                "TXN987654321 BKG0000001"
        );
        String message = objectMapper.writeValueAsString(event);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment);

        // Assert
        verify(bookingService).processBankTransferPayment(eq("BKG0000001"), eq(new BigDecimal("200.00")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should acknowledge message when booking ID cannot be extracted")
    void shouldAcknowledgeWhenBookingIdCannotBeExtracted() throws Exception {
        // Arrange - Invalid transaction details (too short)
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-002",
                "NL91ABNA0417164300",
                new BigDecimal("100.00"),
                "INVALID" // Too short to extract booking ID
        );
        String message = objectMapper.writeValueAsString(event);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge message when processing fails")
    void shouldNotAcknowledgeWhenProcessingFails() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-003",
                "NL91ABNA0417164300",
                new BigDecimal("300.00"),
                "TXN987654321 BKG0000002"
        );
        String message = objectMapper.writeValueAsString(event);

        doThrow(new RuntimeException("Processing error"))
                .when(bookingService).processBankTransferPayment(any(), any());

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment);

        // Assert
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge message when JSON parsing fails")
    void shouldNotAcknowledgeWhenJsonParsingFails() {
        // Arrange
        String invalidJson = "{ invalid json }";

        // Act
        consumer.consumeBankTransferPaymentEvent(invalidJson, acknowledgment);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should extract correct booking ID from transaction details")
    void shouldExtractCorrectBookingId() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-004",
                "DE89370400440532013000",
                new BigDecimal("500.00"),
                "TXN123456789 BKG9999999"
        );
        String message = objectMapper.writeValueAsString(event);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment);

        // Assert
        verify(bookingService).processBankTransferPayment(eq("BKG9999999"), eq(new BigDecimal("500.00")));
        verify(acknowledgment).acknowledge();
    }
}

