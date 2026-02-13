package com.velocity.carservice.application.dto.request;

import com.velocity.carservice.domain.model.PaymentMode;
import com.velocity.carservice.domain.model.VehicleCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookingRequestDTO Validation Tests")
class BookingRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Customer Name Validation")
    class CustomerNameValidationTests {

        @Test
        @DisplayName("Should pass validation for valid customer name")
        void shouldPassForValidCustomerName() {
            BookingRequestDTO request = createValidRequest();
            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation for blank customer name")
        void shouldFailForBlankCustomerName() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Customer name"))).isTrue();
        }

        @Test
        @DisplayName("Should fail validation for customer name less than 2 characters")
        void shouldFailForShortCustomerName() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "J",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Vehicle ID Validation")
    class VehicleIdValidationTests {

        @Test
        @DisplayName("Should fail validation for blank vehicle ID")
        void shouldFailForBlankVehicleId() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Vehicle ID"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Vehicle Category Validation")
    class VehicleCategoryValidationTests {

        @Test
        @DisplayName("Should fail validation for null vehicle category")
        void shouldFailForNullVehicleCategory() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    null,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Vehicle category"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Rental Date Validation")
    class RentalDateValidationTests {

        @Test
        @DisplayName("Should fail validation for null rental start date")
        void shouldFailForNullStartDate() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    null,
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Rental start date"))).isTrue();
        }

        @Test
        @DisplayName("Should fail validation for null rental end date")
        void shouldFailForNullEndDate() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    null,
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Rental end date"))).isTrue();
        }

        @Test
        @DisplayName("Should fail validation for past rental start date")
        void shouldFailForPastStartDate() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(5),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Payment Mode Validation")
    class PaymentModeValidationTests {

        @Test
        @DisplayName("Should fail validation for null payment mode")
        void shouldFailForNullPaymentMode() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    null,
                    "PAY-REF-001",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Payment mode"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Payment Amount Validation")
    class PaymentAmountValidationTests {

        @Test
        @DisplayName("Should fail validation for null payment amount")
        void shouldFailForNullPaymentAmount() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    null
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Payment amount"))).isTrue();
        }

        @Test
        @DisplayName("Should fail validation for zero payment amount")
        void shouldFailForZeroPaymentAmount() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    BigDecimal.ZERO
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation for negative payment amount")
        void shouldFailForNegativePaymentAmount() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "PAY-REF-001",
                    new BigDecimal("-100.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Payment Reference Validation")
    class PaymentReferenceValidationTests {

        @Test
        @DisplayName("Should fail validation for blank payment reference")
        void shouldFailForBlankPaymentReference() {
            BookingRequestDTO request = new BookingRequestDTO(
                    "John Doe",
                    "VH-001",
                    VehicleCategory.SEDAN,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    PaymentMode.DIGITAL_WALLET,
                    "",
                    new BigDecimal("250.00")
            );

            Set<ConstraintViolation<BookingRequestDTO>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().contains("Payment reference"))).isTrue();
        }
    }

    private BookingRequestDTO createValidRequest() {
        return new BookingRequestDTO(
                "John Doe",
                "VH-001",
                VehicleCategory.SEDAN,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10),
                PaymentMode.DIGITAL_WALLET,
                "PAY-REF-001",
                new BigDecimal("250.00")
        );
    }
}

