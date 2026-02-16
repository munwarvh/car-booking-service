package com.velocity.carservice.infrastructure.adapter.inbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.event.BankTransferPaymentEvent;
import com.velocity.carservice.application.service.BookingService;
import com.velocity.carservice.domain.model.ProcessedPaymentEvent;
import com.velocity.carservice.domain.model.ProcessedPaymentEvent.ProcessingStatus;
import com.velocity.carservice.infrastructure.repository.ProcessedPaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankTransferPaymentEventConsumer {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final ProcessedPaymentEventRepository processedPaymentEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(
            topics = "${app.kafka.topics.bank-transfer-payment-events:bank-transfer-payment-events}",
            groupId = "${app.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeBankTransferPaymentEvent(
            String message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received message from topic={}, partition={}, offset={}", topic, partition, offset);
        log.debug("Message payload: {}", message);

        BankTransferPaymentEvent event = null;
        String paymentId = null;
        String bookingId = null;

        try {
            event = deserializeAndValidate(message);
            paymentId = event.paymentId();
            bookingId = event.extractBookingId();

            if (isDuplicateEvent(paymentId)) {
                log.warn("Duplicate payment event detected, skipping. paymentId={}", paymentId);
                acknowledgment.acknowledge();
                return;
            }

            if (bookingId == null || bookingId.isBlank()) {
                handleInvalidBookingId(event, paymentId, message, acknowledgment);
                return;
            }

            log.info("Processing payment: paymentId={}, bookingId={}, amount={}",
                    paymentId, bookingId, event.paymentAmount());

            bookingService.processBankTransferPayment(bookingId, event.paymentAmount());

            recordProcessedEvent(paymentId, bookingId, ProcessingStatus.SUCCESS, null);

            acknowledgment.acknowledge();
            log.info("Successfully processed payment event: paymentId={}, bookingId={}", paymentId, bookingId);

        } catch (JsonProcessingException e) {
            handlePoisonMessage(message, "Invalid JSON format: " + e.getMessage(), acknowledgment);

        } catch (InvalidEventException e) {
            handlePoisonMessage(message, "Schema validation failed: " + e.getMessage(), acknowledgment);

        } catch (Exception e) {
            handleProcessingError(event, paymentId, bookingId, message, e, acknowledgment);
        }
    }

    /**
     * Deserialize and validate the message schema.
     */
    private BankTransferPaymentEvent deserializeAndValidate(String message) throws JsonProcessingException {
        BankTransferPaymentEvent event = objectMapper.readValue(message, BankTransferPaymentEvent.class);

        if (event.paymentId() == null || event.paymentId().isBlank()) {
            throw new InvalidEventException("paymentId is required");
        }
        if (event.paymentAmount() == null || event.paymentAmount().doubleValue() <= 0) {
            throw new InvalidEventException("paymentAmount must be positive");
        }
        if (event.transactionDetails() == null || event.transactionDetails().isBlank()) {
            throw new InvalidEventException("transactionDetails is required");
        }

        return event;
    }

    /**
     * Check if this payment event has already been processed (idempotency check).
     */
    private boolean isDuplicateEvent(String paymentId) {
        return processedPaymentEventRepository.existsByPaymentId(paymentId);
    }

    /**
     * Handle case where booking ID cannot be extracted from transaction details.
     */
    private void handleInvalidBookingId(BankTransferPaymentEvent event, String paymentId,
                                         String originalMessage, Acknowledgment acknowledgment) {
        log.error("Could not extract booking ID from transaction details: {}", event.transactionDetails());

        recordProcessedEvent(paymentId, "UNKNOWN", ProcessingStatus.SKIPPED,
                "Could not extract booking ID from transactionDetails");

        sendToDeadLetterQueue(originalMessage, "Invalid transactionDetails format - cannot extract bookingId");
        acknowledgment.acknowledge();
    }

    /**
     * Handle poison messages (invalid JSON, schema validation failures).
     * These are non-retryable and go directly to DLQ.
     */
    private void handlePoisonMessage(String message, String errorReason, Acknowledgment acknowledgment) {
        log.error("Poison message detected: {}", errorReason);
        sendToDeadLetterQueue(message, errorReason);
        acknowledgment.acknowledge();
    }

    /**
     * Handle processing errors with retry logic.
     */
    private void handleProcessingError(BankTransferPaymentEvent event, String paymentId,
                                        String bookingId, String originalMessage,
                                        Exception e, Acknowledgment acknowledgment) {
        log.error("Failed to process payment event: paymentId={}, error={}", paymentId, e.getMessage(), e);

        if (paymentId != null) {
            recordProcessedEvent(paymentId, bookingId != null ? bookingId : "UNKNOWN",
                    ProcessingStatus.FAILED, e.getMessage());
        }

        sendToDeadLetterQueue(originalMessage, "Processing failed: " + e.getMessage());

        acknowledgment.acknowledge();
    }

    private void recordProcessedEvent(String paymentId, String bookingId,
                                       ProcessingStatus status, String errorMessage) {
        try {
            ProcessedPaymentEvent record = ProcessedPaymentEvent.builder()
                    .paymentId(paymentId)
                    .bookingId(bookingId)
                    .status(status)
                    .errorMessage(errorMessage != null ?
                            errorMessage.substring(0, Math.min(errorMessage.length(), 1000)) : null)
                    .build();

            processedPaymentEventRepository.save(record);
            log.debug("Recorded processed event: paymentId={}, status={}", paymentId, status);

        } catch (Exception e) {
            log.error("Failed to record processed event: paymentId={}, error={}", paymentId, e.getMessage());
        }
    }

    /**
     * Send failed message to Dead Letter Queue.
     */
    private void sendToDeadLetterQueue(String message, String errorReason) {
        try {
            String dlqMessage = String.format("{\"originalMessage\": %s, \"errorReason\": \"%s\", \"timestamp\": \"%s\"}",
                    message, errorReason.replace("\"", "'"), java.time.Instant.now());

            kafkaTemplate.send("bank-transfer-payment-events-dlq", dlqMessage);
            log.info("Message sent to DLQ: {}", errorReason);

        } catch (Exception e) {
            log.error("Failed to send message to DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Dead letter queue consumer for failed messages.
     * Messages here require manual intervention.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.bank-transfer-payment-events-dlq:bank-transfer-payment-events-dlq}",
            groupId = "${app.kafka.consumer-group}-dlq"
    )
    public void consumeDeadLetterQueue(String message) {
        log.error("DLQ Message received - manual intervention required: {}", message);
    }

    private static class InvalidEventException extends RuntimeException {
        public InvalidEventException(String message) {
            super(message);
        }
    }
}
