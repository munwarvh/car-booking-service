package com.velocity.carservice.infrastructure.adapter.inbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.event.BankTransferPaymentEvent;
import com.velocity.carservice.application.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for bank-transfer-payment-events topic
 * Listens for bank transfer payment confirmations and updates booking status
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BankTransferPaymentEventConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.bank-transfer-payment-events:bank-transfer-payment-events}",
            groupId = "${app.kafka.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBankTransferPaymentEvent(String message, Acknowledgment acknowledgment) {
        log.info("Received bank transfer payment event: {}", message);

        try {
            BankTransferPaymentEvent event = objectMapper.readValue(message, BankTransferPaymentEvent.class);

            // Extract booking ID from transaction details
            String bookingId = event.extractBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                log.error("Could not extract booking ID from transaction details: {}", event.transactionDetails());
                acknowledgment.acknowledge();
                return;
            }

            log.info("Processing payment for booking: {}, amount: {}, paymentId: {}",
                    bookingId, event.paymentAmount(), event.paymentId());

            // Process the payment
            bookingService.processBankTransferPayment(bookingId, event.paymentAmount());

            acknowledgment.acknowledge();
            log.info("Bank transfer payment event processed successfully for booking: {}", bookingId);

        } catch (Exception e) {
            log.error("Failed to process bank transfer payment event: {}", e.getMessage(), e);
        }
    }

    /**
     * Dead letter queue consumer for failed messages
     */
    @KafkaListener(
            topics = "${app.kafka.topics.bank-transfer-payment-events-dlq:bank-transfer-payment-events-dlq}",
            groupId = "${app.kafka.consumer-group}-dlq"
    )
    public void consumeDeadLetterQueue(String message) {
        log.error("Received message in DLQ - manual intervention required: {}", message);
    }
}
