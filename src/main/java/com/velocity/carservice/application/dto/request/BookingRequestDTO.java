package com.velocity.carservice.application.dto.request;

import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRequestDTO(
        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
        String customerName,

        @NotBlank(message = "Vehicle ID is required")
        String vehicleId,

        @NotNull(message = "Vehicle category is required")
        VehicleCategory vehicleCategory,

        @NotNull(message = "Rental start date is required")
        @FutureOrPresent(message = "Rental start date must be today or in the future")
        LocalDate rentalStartDate,

        @NotNull(message = "Rental end date is required")
        @Future(message = "Rental end date must be in the future")
        LocalDate rentalEndDate,

        @NotNull(message = "Payment mode is required")
        PaymentMode paymentMode,

        @NotBlank(message = "Payment reference is required")
        String paymentReference,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Invalid amount format")
        BigDecimal paymentAmount
) {
}
