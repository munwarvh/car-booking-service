package com.velocity.carservice.infrastructure.adapter.inbound.rest;

import com.velocity.carservice.application.dto.request.BookingRequestDTO;
import com.velocity.carservice.application.dto.response.BookingResponseDTO;
import com.velocity.carservice.application.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Booking", description = "Car Rental Booking API - Velocity Motors")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Confirm a car booking",
            description = "Creates and confirms a car rental booking based on payment mode")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
            @ApiResponse(responseCode = "422", description = "Payment validation failed")
    })
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request) {
        log.info("Received booking confirmation request for customer: {}", request.customerName());
        BookingResponseDTO response = bookingService.confirmBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking by ID", description = "Retrieves booking details by booking ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDTO> getBooking(@PathVariable String bookingId) {
        log.info("Fetching booking: {}", bookingId);
        BookingResponseDTO response = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel a booking", description = "Cancels an existing booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking cannot be cancelled")
    })
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable String bookingId) {
        log.info("Cancelling booking: {}", bookingId);
        BookingResponseDTO response = bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(response);
    }
}
