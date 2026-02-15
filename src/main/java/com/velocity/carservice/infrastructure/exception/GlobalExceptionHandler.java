package com.velocity.carservice.infrastructure.exception;

import com.velocity.carservice.infrastructure.exception.CustomExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Car Booking Service.
 * Provides consistent error responses across all endpoints.
 *
 * Exception categories:
 * - Business exceptions: Expected errors from business rule violations (4xx)
 * - System exceptions: Unexpected errors from infrastructure/external services (5xx)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== BUSINESS EXCEPTIONS (4xx) ====================

    /**
     * Handle booking not found - 404
     */
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFoundException(
            BookingNotFoundException ex, HttpServletRequest request) {
        log.warn("Booking not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(),
                ErrorCode.BOOKING_NOT_FOUND, request.getRequestURI());
    }

    /**
     * Handle booking validation errors - 400
     */
    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ErrorResponse> handleBookingValidationException(
            BookingValidationException ex, HttpServletRequest request) {
        log.warn("Booking validation failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                ErrorCode.VALIDATION_ERROR, request.getRequestURI());
    }

    /**
     * Handle payment rejection/failure - 422
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailedException(
            PaymentFailedException ex, HttpServletRequest request) {
        log.warn("Payment failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                ErrorCode.PAYMENT_REJECTED, request.getRequestURI());
    }

    /**
     * Handle invalid booking state transitions - 409
     */
    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookingStateException(
            InvalidBookingStateException ex, HttpServletRequest request) {
        log.warn("Invalid booking state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                ErrorCode.INVALID_BOOKING_STATE, request.getRequestURI());
    }

    /**
     * Handle unsupported payment mode - 400
     */
    @ExceptionHandler(UnsupportedPaymentModeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedPaymentModeException(
            UnsupportedPaymentModeException ex, HttpServletRequest request) {
        log.warn("Unsupported payment mode: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                ErrorCode.UNSUPPORTED_PAYMENT_MODE, request.getRequestURI());
    }

    /**
     * Handle Spring validation errors - 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Request validation error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_ERROR.name(),
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal state - 409
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                ErrorCode.INVALID_BOOKING_STATE, request.getRequestURI());
    }

    /**
     * Handle illegal argument - 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                ErrorCode.VALIDATION_ERROR, request.getRequestURI());
    }

    // ==================== SYSTEM EXCEPTIONS (5xx) ====================

    /**
     * Handle credit card service unavailable - 503
     */
    @ExceptionHandler(CreditCardServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCreditCardServiceUnavailableException(
            CreditCardServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Credit card service unavailable: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Credit card validation service is temporarily unavailable. Please try again later.",
                ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, request.getRequestURI());
    }

    /**
     * Handle all other unexpected exceptions - 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

    // ==================== HELPER METHODS ====================

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, ErrorCode errorCode, String path) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode.name(),
                message,
                path
        );
        return ResponseEntity.status(status).body(response);
    }

    // ==================== ERROR CODES ====================

    /**
     * Enum of all error codes for consistent error identification.
     * Clients can use these codes for programmatic error handling.
     */
    public enum ErrorCode {
        // Business errors (4xx)
        BOOKING_NOT_FOUND,
        VALIDATION_ERROR,
        PAYMENT_REJECTED,
        INVALID_BOOKING_STATE,
        UNSUPPORTED_PAYMENT_MODE,

        // System errors (5xx)
        EXTERNAL_SERVICE_UNAVAILABLE,
        INTERNAL_SERVER_ERROR
    }

    // ==================== RESPONSE DTOs ====================

    /**
     * Standard error response structure.
     * Example:
     * {
     *   "timestamp": "2026-02-14T10:20:30",
     *   "status": 422,
     *   "errorCode": "PAYMENT_REJECTED",
     *   "message": "Credit card payment was rejected",
     *   "path": "/api/v1/bookings/confirm"
     * }
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String errorCode,
            String message,
            String path
    ) {}

    /**
     * Validation error response with field-level details.
     */
    public record ValidationErrorResponse(
            LocalDateTime timestamp,
            int status,
            String errorCode,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {}
}
