package com.velocity.carservice.infrastructure.adapter.inbound.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocity.carservice.application.dto.request.BookingRequestDTO;
import com.velocity.carservice.application.dto.response.BookingResponseDTO;
import com.velocity.carservice.application.service.BookingService;
import com.velocity.carservice.domain.model.BookingStatus;
import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.BookingNotFoundException;
import com.velocity.carservice.infrastructure.exception.CustomExceptions.PaymentFailedException;
import com.velocity.carservice.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingController Unit Tests")
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/bookings - Create Booking")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking with CONFIRMED status for digital wallet")
        void shouldCreateConfirmedBookingForDigitalWallet() throws Exception {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "WALLET-123");

            BookingResponseDTO response = new BookingResponseDTO("BKG0000001", BookingStatus.CONFIRMED);
            when(bookingService.confirmBooking(any(BookingRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bookingId").value("BKG0000001"))
                    .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Should create booking with PENDING_PAYMENT status for bank transfer")
        void shouldCreatePendingBookingForBankTransfer() throws Exception {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Alice Brown",
                    "VH-002",
                    VehicleCategory.COMPACT,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.BANK_TRANSFER,
                    "BT-REF-001");

            BookingResponseDTO response = new BookingResponseDTO("BKG0000002", BookingStatus.PENDING_PAYMENT);
            when(bookingService.confirmBooking(any(BookingRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bookingId").value("BKG0000002"))
                    .andExpect(jsonPath("$.bookingStatus").value("PENDING_PAYMENT"));
        }

        @Test
        @DisplayName("Should return 422 when credit card payment fails")
        void shouldReturn422WhenCreditCardPaymentFails() throws Exception {
            // Arrange
            BookingRequestDTO request = new BookingRequestDTO(
                    "Bob Wilson",
                    "VH-003",
                    VehicleCategory.LUXURY,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.CREDIT_CARD,
                    "REJECT-123");

            when(bookingService.confirmBooking(any(BookingRequestDTO.class)))
                    .thenThrow(new PaymentFailedException("Credit card payment was not approved"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode").value("PAYMENT_REJECTED"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            // Arrange - missing customerName
            String invalidRequest = """
                    {
                        "vehicleId": "VH-001",
                        "vehicleCategory": "SEDAN",
                        "rentalStartDate": "2026-03-01",
                        "rentalEndDate": "2026-03-05",
                        "paymentMode": "DIGITAL_WALLET",
                        "paymentReference": "PAY-001"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookings/{bookingId} - Get Booking")
    class GetBookingTests {

        @Test
        @DisplayName("Should return booking when found")
        void shouldReturnBookingWhenFound() throws Exception {
            // Arrange
            BookingResponseDTO response = new BookingResponseDTO("BKG0000001", BookingStatus.CONFIRMED);
            when(bookingService.getBookingById("BKG0000001")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/bookings/BKG0000001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value("BKG0000001"))
                    .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Should return 404 when booking not found")
        void shouldReturn404WhenBookingNotFound() throws Exception {
            // Arrange
            when(bookingService.getBookingById("INVALID"))
                    .thenThrow(new BookingNotFoundException("Booking not found: INVALID"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/bookings/INVALID"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/bookings/{bookingId} - Cancel Booking")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void shouldCancelBookingSuccessfully() throws Exception {
            // Arrange
            BookingResponseDTO response = new BookingResponseDTO("BKG0000001", BookingStatus.CANCELLED);
            when(bookingService.cancelBooking("BKG0000001")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/BKG0000001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value("BKG0000001"))
                    .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 404 when booking to cancel not found")
        void shouldReturn404WhenCancellingNonExistentBooking() throws Exception {
            // Arrange
            when(bookingService.cancelBooking("INVALID"))
                    .thenThrow(new BookingNotFoundException("Booking not found: INVALID"));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/bookings/INVALID"))
                    .andExpect(status().isNotFound());
        }
    }
}
