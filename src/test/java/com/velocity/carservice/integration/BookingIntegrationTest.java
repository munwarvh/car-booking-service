package com.velocity.carservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.event.BankTransferPaymentEvent;
import com.velocity.carservice.application.dto.request.BookingRequestDTO;
import com.velocity.carservice.application.dto.response.BookingResponseDTO;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Car Booking Service Integration Tests")
@Tag("integration")
class BookingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("car_booking_test_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Flyway
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.topics.bank-transfer-payment-events", () -> "bank-transfer-payment-events");

        // Disable Redis for tests
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");

        // Mock credit card service URL (will use WireMock or mock in test)
        registry.add("app.external-services.credit-card-validation.url", () -> "http://localhost:9999");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/bookings";
    }

    @Nested
    @DisplayName("Digital Wallet Payment Integration Tests")
    class DigitalWalletIntegrationTests {

        @Test
        @DisplayName("Should create and confirm booking immediately for digital wallet payment")
        void shouldCreateConfirmedBookingForDigitalWallet() {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Integration Test User",
                    "VH-INT-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "WALLET-INT-001",
                    new BigDecimal("299.99")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            // Act
            ResponseEntity<BookingResponseDTO> response = restTemplate.postForEntity(
                    baseUrl, entity, BookingResponseDTO.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().bookingId()).startsWith("BKG");
            assertThat(response.getBody().bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Bank Transfer Payment Integration Tests")
    class BankTransferIntegrationTests {

        @Test
        @DisplayName("Should create booking with PENDING_PAYMENT status for bank transfer")
        void shouldCreatePendingBookingForBankTransfer() {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Bank Transfer User",
                    "VH-INT-002",
                    VehicleCategory.SUV,
                    LocalDate.now().plusDays(7),
                    LocalDate.now().plusDays(14),
                    PaymentMode.BANK_TRANSFER,
                    "BT-INT-001",
                    new BigDecimal("450.00")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            // Act
            ResponseEntity<BookingResponseDTO> response = restTemplate.postForEntity(
                    baseUrl, entity, BookingResponseDTO.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().bookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        }

        @Test
        @DisplayName("Should confirm booking when bank transfer payment event is received via Kafka")
        void shouldConfirmBookingWhenPaymentEventReceived() throws Exception {
            // Arrange - Create a pending booking first
            BookingRequestDTO request = new BookingRequestDTO(
                    "Kafka Test User",
                    "VH-INT-003",
                    VehicleCategory.COMPACT,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15),
                    PaymentMode.BANK_TRANSFER,
                    "BT-KAFKA-001",
                    new BigDecimal("200.00")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BookingResponseDTO> createResponse = restTemplate.postForEntity(
                    baseUrl, entity, BookingResponseDTO.class);

            assertThat(createResponse.getBody()).isNotNull();
            String bookingId = createResponse.getBody().bookingId();
            assertThat(createResponse.getBody().bookingStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

            // Act - Send payment event via Kafka
            KafkaTemplate<String, String> kafkaTemplate = createKafkaTemplate();

            BankTransferPaymentEvent paymentEvent = new BankTransferPaymentEvent(
                    "PAY-KAFKA-001",
                    "NL91ABNA0417164300",
                    new BigDecimal("200.00"),
                    "TXN987654321 " + bookingId
            );

            String eventJson = objectMapper.writeValueAsString(paymentEvent);
            kafkaTemplate.send("bank-transfer-payment-events", bookingId, eventJson).get();

            // Assert - Wait for the booking to be confirmed
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                ResponseEntity<BookingResponseDTO> getResponse = restTemplate.getForEntity(
                        baseUrl + "/" + bookingId, BookingResponseDTO.class);

                assertThat(getResponse.getBody()).isNotNull();
                assertThat(getResponse.getBody().bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
            });
        }
    }

    @Nested
    @DisplayName("Get Booking Integration Tests")
    class GetBookingIntegrationTests {

        @Test
        @DisplayName("Should retrieve existing booking by ID")
        void shouldRetrieveExistingBooking() {
            // Arrange - Create a booking first
            BookingRequestDTO request = new BookingRequestDTO(
                    "Get Test User",
                    "VH-INT-004",
                    VehicleCategory.LUXURY,
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(5),
                    PaymentMode.DIGITAL_WALLET,
                    "WALLET-GET-001",
                    new BigDecimal("599.99")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BookingResponseDTO> createResponse = restTemplate.postForEntity(
                    baseUrl, entity, BookingResponseDTO.class);

            String bookingId = createResponse.getBody().bookingId();

            // Act
            ResponseEntity<BookingResponseDTO> getResponse = restTemplate.getForEntity(
                    baseUrl + "/" + bookingId, BookingResponseDTO.class);

            // Assert
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getResponse.getBody()).isNotNull();
            assertThat(getResponse.getBody().bookingId()).isEqualTo(bookingId);
            assertThat(getResponse.getBody().bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should return 404 for non-existent booking")
        void shouldReturn404ForNonExistentBooking() {
            // Act
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/NONEXISTENT", String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Cancel Booking Integration Tests")
    class CancelBookingIntegrationTests {

        @Test
        @DisplayName("Should cancel pending booking successfully")
        void shouldCancelPendingBooking() {
            // Arrange - Create a pending booking
            BookingRequestDTO request = new BookingRequestDTO(
                    "Cancel Test User",
                    "VH-INT-005",
                    VehicleCategory.COMPACT,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(8),
                    PaymentMode.BANK_TRANSFER,
                    "BT-CANCEL-001",
                    new BigDecimal("150.00")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BookingResponseDTO> createResponse = restTemplate.postForEntity(
                    baseUrl, entity, BookingResponseDTO.class);

            String bookingId = createResponse.getBody().bookingId();

            // Act
            ResponseEntity<BookingResponseDTO> cancelResponse = restTemplate.exchange(
                    baseUrl + "/" + bookingId,
                    HttpMethod.DELETE,
                    null,
                    BookingResponseDTO.class);

            // Assert
            assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(cancelResponse.getBody()).isNotNull();
            assertThat(cancelResponse.getBody().bookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationIntegrationTests {

        @Test
        @DisplayName("Should reject booking exceeding 21 days")
        void shouldRejectBookingExceeding21Days() {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Validation Test User",
                    "VH-INT-006",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(30), // 25 days - exceeds 21 day limit
                    PaymentMode.DIGITAL_WALLET,
                    "WALLET-VAL-001",
                    new BigDecimal("1000.00")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl, entity, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("21 days");
        }

        @Test
        @DisplayName("Should reject booking with end date before start date")
        void shouldRejectBookingWithInvalidDates() {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Invalid Date User",
                    "VH-INT-007",
                    VehicleCategory.SUV,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(5), // End date before start date
                    PaymentMode.DIGITAL_WALLET,
                    "WALLET-VAL-002",
                    new BigDecimal("500.00")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl, entity, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should reject booking with missing required fields")
        void shouldRejectBookingWithMissingFields() {
            // Arrange - Missing customerName
            String invalidRequest = """
                    {
                        "vehicleId": "VH-INT-008",
                        "vehicleCategory": "SEDAN",
                        "rentalStartDate": "2026-03-01",
                        "rentalEndDate": "2026-03-05",
                        "paymentMode": "DIGITAL_WALLET",
                        "paymentReference": "WALLET-VAL-003",
                        "paymentAmount": 250.00
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(invalidRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl, entity, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    private KafkaTemplate<String, String> createKafkaTemplate() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }
}
