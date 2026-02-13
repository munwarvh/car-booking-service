package com.velocity.carservice.infrastructure.exception;

public class CustomExceptions {

    public static class BookingNotFoundException extends RuntimeException {
        public BookingNotFoundException(String message) {
            super(message);
        }
    }

    public static class BookingValidationException extends RuntimeException {
        public BookingValidationException(String message) {
            super(message);
        }
    }

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) {
            super(message);
        }
    }

    public static class InvalidBookingStateException extends RuntimeException {
        public InvalidBookingStateException(String message) {
            super(message);
        }
    }
}
