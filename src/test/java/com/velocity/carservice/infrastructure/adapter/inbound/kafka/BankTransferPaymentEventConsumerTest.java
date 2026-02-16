package com.velocity.carservice.infrastructure.adapter.inbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.event.BankTransferPaymentEvent;
import com.velocity.carservice.application.service.BookingService;
import com.velocity.carservice.infrastructure.repository.ProcessedPaymentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankTransferPaymentEventConsumer Unit Tests")
class BankTransferPaymentEventConsumerTest {

    private static final String TEST_TOPIC = "bank-transfer-payment-events";
    private static final int TEST_PARTITION = 0;
    private static final long TEST_OFFSET = 100L;

    @Mock
    private BookingService bookingService;

    @Mock
    private ProcessedPaymentEventRepository processedPaymentEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

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

        when(processedPaymentEventRepository.existsByPaymentId("PAY-001")).thenReturn(false);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService).processBankTransferPayment(eq("BKG0000001"), eq(new BigDecimal("200.00")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should skip duplicate payment event (idempotency)")
    void shouldSkipDuplicatePaymentEvent() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-DUPLICATE",
                "NL91ABNA0417164300",
                new BigDecimal("200.00"),
                "TXN987654321 BKG0000001"
        );
        String message = objectMapper.writeValueAsString(event);

        when(processedPaymentEventRepository.existsByPaymentId("PAY-DUPLICATE")).thenReturn(true);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(acknowledgment).acknowledge(); // Still ack to move past duplicate
    }

    @Test
    @DisplayName("Should send to DLQ when booking ID cannot be extracted")
    void shouldSendToDlqWhenBookingIdCannotBeExtracted() throws Exception {
        // Arrange - Invalid transaction details (too short)
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-002",
                "NL91ABNA0417164300",
                new BigDecimal("100.00"),
                "INVALID" // Too short to extract booking ID
        );
        String message = objectMapper.writeValueAsString(event);

        when(processedPaymentEventRepository.existsByPaymentId("PAY-002")).thenReturn(false);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(kafkaTemplate).send(eq("bank-transfer-payment-events-dlq"), any(String.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should send to DLQ and acknowledge when processing fails")
    void shouldSendToDlqWhenProcessingFails() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-003",
                "NL91ABNA0417164300",
                new BigDecimal("300.00"),
                "TXN987654321 BKG0000002"
        );
        String message = objectMapper.writeValueAsString(event);

        when(processedPaymentEventRepository.existsByPaymentId("PAY-003")).thenReturn(false);
        doThrow(new RuntimeException("Processing error"))
                .when(bookingService).processBankTransferPayment(any(), any());

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(kafkaTemplate).send(eq("bank-transfer-payment-events-dlq"), any(String.class));
        verify(acknowledgment).acknowledge(); // Ack after sending to DLQ
    }

    @Test
    @DisplayName("Should send to DLQ when JSON parsing fails (poison message)")
    void shouldSendToDlqWhenJsonParsingFails() {
        // Arrange
        String invalidJson = "{ invalid json }";

        // Act
        consumer.consumeBankTransferPaymentEvent(invalidJson, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(kafkaTemplate).send(eq("bank-transfer-payment-events-dlq"), any(String.class));
        verify(acknowledgment).acknowledge(); // Ack poison message after DLQ
    }

    @Test
    @DisplayName("Should send to DLQ when schema validation fails")
    void shouldSendToDlqWhenSchemaValidationFails() throws Exception {
        // Arrange - Missing required paymentId
        String messageWithNullPaymentId = "{\"paymentId\": null, \"senderAccountNumber\": \"NL91ABNA\", \"paymentAmount\": 100.00, \"transactionDetails\": \"TXN123 BKG001\"}";

        // Act
        consumer.consumeBankTransferPaymentEvent(messageWithNullPaymentId, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService, never()).processBankTransferPayment(any(), any());
        verify(kafkaTemplate).send(eq("bank-transfer-payment-events-dlq"), any(String.class));
        verify(acknowledgment).acknowledge();
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

        when(processedPaymentEventRepository.existsByPaymentId("PAY-004")).thenReturn(false);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(bookingService).processBankTransferPayment(eq("BKG9999999"), eq(new BigDecimal("500.00")));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should record processed event for successful processing")
    void shouldRecordProcessedEventOnSuccess() throws Exception {
        // Arrange
        BankTransferPaymentEvent event = new BankTransferPaymentEvent(
                "PAY-005",
                "NL91ABNA0417164300",
                new BigDecimal("250.00"),
                "TXN987654321 BKG0000005"
        );
        String message = objectMapper.writeValueAsString(event);

        when(processedPaymentEventRepository.existsByPaymentId("PAY-005")).thenReturn(false);

        // Act
        consumer.consumeBankTransferPaymentEvent(message, acknowledgment, TEST_TOPIC, TEST_PARTITION, TEST_OFFSET);

        // Assert
        verify(processedPaymentEventRepository).save(any());
        verify(acknowledgment).acknowledge();
    }
}
