package com.velocity.carservice.application.dto.response;

import com.velocity.carservice.domain.model.BookingStatus;

public record BookingResponseDTO(
        String bookingId,
        BookingStatus bookingStatus
) {
}
