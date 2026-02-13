# Car Rental Booking Service

A production-grade car rental booking service built with Spring Boot for Velocity Motors.

## Features

- **REST API** for car booking confirmation
- **Multiple Payment Modes**: Digital Wallet (instant), Credit Card (validation required), Bank Transfer (pending)
- **Kafka Integration**: Listens for bank transfer payment events
- **Automatic Cancellation**: Unpaid bank transfer bookings cancelled 48 hours before rental start
- **Circuit Breaker**: Resilience4j for external service calls

## Prerequisites

- Java 17+
- Docker & Docker Compose
- Gradle 8.x (or use the included wrapper)

## Quick Start

### Option 1: Run Everything in Docker (Recommended)

```bash
# Navigate to docker folder
cd docker

# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

This starts:
- **Car Booking Service**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Kafka**: localhost:9092
- **Kafka UI**: http://localhost:8090
- **Redis**: localhost:6379
- **Mock Credit Card Service**: http://localhost:9090

### Option 2: Run App Locally with Docker Infrastructure

```bash
# Start only infrastructure services
cd docker
docker-compose -f docker-compose-local.yml up -d

# Run the Spring Boot app locally (from project root)
cd ..
.\gradlew.bat bootRun
```

### Option 3: Run with H2 (In-Memory Database)

```bash
# Run with dev profile (uses H2, no external dependencies except Kafka)
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

## API Endpoints

### Confirm Booking
```bash
POST http://localhost:8080/api/v1/bookings/confirm
Content-Type: application/json

{
  "customerName": "John Doe",
  "vehicleId": "VH-12345",
  "vehicleCategory": "SEDAN",
  "rentalStartDate": "2026-02-20",
  "rentalEndDate": "2026-02-25",
  "paymentMode": "DIGITAL_WALLET",
  "paymentReference": "PAY-REF-001",
  "paymentAmount": 250.00
}
```

### Payment Modes

| Mode | Behavior |
|------|----------|
| `DIGITAL_WALLET` | Booking confirmed immediately |
| `CREDIT_CARD` | Validates with credit-card-validation-service, confirms if APPROVED |
| `BANK_TRANSFER` | Creates booking with `PENDING_PAYMENT` status |

### Get Booking
```bash
GET http://localhost:8080/api/v1/bookings/{bookingId}
```

### Cancel Booking
```bash
POST http://localhost:8080/api/v1/bookings/{bookingId}/cancel
```

## Testing Examples

### Test Digital Wallet Payment (Instant Confirmation)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "vehicleId": "VH-001",
    "vehicleCategory": "SEDAN",
    "rentalStartDate": "2026-02-20",
    "rentalEndDate": "2026-02-25",
    "paymentMode": "DIGITAL_WALLET",
    "paymentReference": "WALLET-123",
    "paymentAmount": 250.00
  }'
```

### Test Credit Card Payment (Approved)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "vehicleId": "VH-002",
    "vehicleCategory": "SUV",
    "rentalStartDate": "2026-02-20",
    "rentalEndDate": "2026-02-25",
    "paymentMode": "CREDIT_CARD",
    "paymentReference": "CC-APPROVED-123",
    "paymentAmount": 450.00
  }'
```

### Test Credit Card Payment (Rejected)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Bob Wilson",
    "vehicleId": "VH-003",
    "vehicleCategory": "LUXURY",
    "rentalStartDate": "2026-02-20",
    "rentalEndDate": "2026-02-25",
    "paymentMode": "CREDIT_CARD",
    "paymentReference": "REJECT-123",
    "paymentAmount": 800.00
  }'
```

### Test Bank Transfer (Pending Payment)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice Brown",
    "vehicleId": "VH-004",
    "vehicleCategory": "COMPACT",
    "rentalStartDate": "2026-02-20",
    "rentalEndDate": "2026-02-25",
    "paymentMode": "BANK_TRANSFER",
    "paymentReference": "BT-REF-001",
    "paymentAmount": 150.00
  }'
```

### Simulate Bank Transfer Payment via Kafka

Use Kafka UI at http://localhost:8090 or command line:

```bash
# Connect to Kafka container
docker exec -it car-booking-kafka bash

# Send payment event (replace BKG0000001 with actual booking ID)
kafka-console-producer --broker-list localhost:9092 --topic bank-transfer-payment-events

# Paste this JSON and press Enter:
{"paymentId":"PAY-001","senderAccountNumber":"NL91ABNA0417164300","paymentAmount":150.00,"transactionDetails":"TXN987654321 BKG0000001"}
```

## Validation Rules

- Vehicle cannot be booked for more than **21 days**
- Rental end date must be **after** the rental start date
- Bank transfer bookings are **auto-cancelled** if not paid 48 hours before rental start

## Project Structure

```
src/main/java/com/velocity/carservice/
├── CarBookingApplication.java
├── config/                    # Configuration (Kafka, Redis, WebClient, etc.)
├── domain/
│   ├── model/                # Entities (Booking, enums)
│   ├── repository/           # Repository interfaces
│   └── service/              # Domain services
├── application/
│   ├── dto/                  # Request/Response DTOs
│   └── service/              # Application services
├── infrastructure/
│   ├── adapter/
│   │   ├── inbound/          # REST controllers, Kafka consumers
│   │   └── outbound/         # External service clients
│   ├── repository/           # JPA repository implementations
│   └── exception/            # Exception handling
└── shared/                   # Constants, utilities
```

## Monitoring

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Kafka UI**: http://localhost:8090

## Docker Commands

```bash
# Start all services
cd docker && docker-compose up -d

# View logs
docker-compose logs -f car-booking-service

# Stop all services
docker-compose down

# Stop and remove volumes (reset data)
docker-compose down -v

# Rebuild after code changes
docker-compose up --build
```

## Building

```bash
# Build JAR
.\gradlew.bat build

# Build without tests
.\gradlew.bat build -x test

# Run tests
.\gradlew.bat test
```
