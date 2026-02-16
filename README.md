# 🚗 Car Booking Service

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A robust car rental booking microservice built with **Spring Boot** for **Velocity Motors**. This service handles booking confirmations with multiple payment modes, real-time payment event processing via Kafka, and automated booking lifecycle management.

---

## 📑 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
  - [High-Level Architecture](#high-level-architecture)
  - [Hexagonal Architecture](#hexagonal-architecture)
  - [Payment Flow](#payment-flow)
  - [Bank Transfer Payment Flow](#bank-transfer-payment-flow)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
  - [Option 1: Docker Compose (Recommended)](#option-1-docker-compose-recommended)
  - [Option 2: Local Development](#option-2-local-development)
  - [Option 3: Minimal Setup (H2)](#option-3-minimal-setup-h2)
- [API Reference](#-api-reference)
  - [Endpoints](#endpoints)
  - [Payment Modes](#payment-modes)
  - [Vehicle Categories](#vehicle-categories)
  - [Booking Statuses](#booking-statuses)
- [Configuration](#-configuration)
  - [Environment Variables](#environment-variables)
  - [Application Profiles](#application-profiles)
- [Testing](#-testing)
  - [Running Tests](#running-tests)
  - [API Testing Examples](#api-testing-examples)
- [Observability](#-observability)
  - [Health Checks](#health-checks)
  - [Metrics](#metrics)
  - [Distributed Tracing](#distributed-tracing)
- [Database Migrations](#-database-migrations)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Multi-Payment Support** | Digital Wallet (instant), Credit Card (validated), Bank Transfer (async) |
| **Event-Driven Architecture** | Kafka integration for bank transfer payment events |
| **Auto-Cancellation** | Scheduled job cancels unpaid bookings 48h before rental start |
| **Circuit Breaker** | Resilience4j for fault-tolerant external service calls |
| **Distributed Locking** | ShedLock prevents duplicate scheduled task execution |
| **Redis Caching** | Improves read performance with configurable TTL |
| **API Documentation** | Interactive Swagger UI with OpenAPI 3.0 spec |
| **Observability** | Prometheus metrics, health checks, distributed tracing |
| **Database Migrations** | Version-controlled schema with Flyway |
| **Idempotent Processing** | Duplicate payment event detection |

---

## 🏗 Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CAR BOOKING SERVICE                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                     │
│  │   REST API   │     │    Kafka     │     │  Scheduler   │                     │
│  │  Controller  │     │   Consumer   │     │   (Cron)     │                     │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                     │
│         │                    │                    │                              │
│         └────────────────────┼────────────────────┘                              │
│                              │                                                   │
│                    ┌─────────▼─────────┐                                        │
│                    │  Application      │                                        │
│                    │  Services         │                                        │
│                    │  ┌─────────────┐  │                                        │
│                    │  │  Payment    │  │                                        │
│                    │  │  Strategies │  │                                        │
│                    │  └─────────────┘  │                                        │
│                    └─────────┬─────────┘                                        │
│                              │                                                   │
│                    ┌─────────▼─────────┐                                        │
│                    │  Domain Layer     │                                        │
│                    │  (Business Logic) │                                        │
│                    └─────────┬─────────┘                                        │
│                              │                                                   │
│         ┌────────────────────┼────────────────────┐                              │
│         │                    │                    │                              │
│  ┌──────▼───────┐    ┌──────▼───────┐    ┌──────▼───────┐                       │
│  │  PostgreSQL  │    │    Redis     │    │   External   │                       │
│  │  Repository  │    │    Cache     │    │   Services   │                       │
│  └──────────────┘    └──────────────┘    └──────────────┘                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Hexagonal Architecture

The service follows **Hexagonal (Ports & Adapters) Architecture**:

```
┌─────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    INBOUND ADAPTERS                         ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ││
│  │  │ REST API    │  │ Kafka       │  │ Scheduler           │  ││
│  │  │ Controller  │  │ Consumer    │  │ (BookingCancellation│  ││
│  │  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  ││
│  └─────────┼────────────────┼────────────────────┼─────────────┘│
│            │                │                    │               │
│  ┌─────────▼────────────────▼────────────────────▼─────────────┐│
│  │                    APPLICATION LAYER                        ││
│  │  ┌─────────────────┐  ┌──────────────────────────────────┐  ││
│  │  │ BookingService  │  │ PaymentStrategyFactory           │  ││
│  │  │                 │  │  ├─ DigitalWalletPaymentStrategy │  ││
│  │  │                 │  │  ├─ CreditCardPaymentStrategy    │  ││
│  │  │                 │  │  └─ BankTransferPaymentStrategy  │  ││
│  │  └────────┬────────┘  └──────────────────────────────────┘  ││
│  └───────────┼─────────────────────────────────────────────────┘│
│              │                                                   │
│  ┌───────────▼─────────────────────────────────────────────────┐│
│  │                      DOMAIN LAYER                           ││
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  ││
│  │  │ Booking         │  │ BookingDomain   │  │ Booking     │  ││
│  │  │ (Entity)        │  │ Service         │  │ Repository  │  ││
│  │  └─────────────────┘  └─────────────────┘  │ (Port)      │  ││
│  │                                            └─────────────┘  ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    OUTBOUND ADAPTERS                        ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ││
│  │  │ JPA         │  │ Redis       │  │ CreditCard          │  ││
│  │  │ Repository  │  │ Cache       │  │ ValidationClient    │  ││
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Payment Flow

```
┌────────────┐      ┌─────────────────┐      ┌──────────────────┐
│   Client   │      │  Booking API    │      │ Payment Strategy │
└─────┬──────┘      └────────┬────────┘      └────────┬─────────┘
      │                      │                        │
      │  POST /bookings      │                        │
      │─────────────────────>│                        │
      │                      │                        │
      │                      │  Process Payment       │
      │                      │───────────────────────>│
      │                      │                        │
      │                      │      ┌─────────────────┴─────────────────┐
      │                      │      │                                   │
      │                      │      ▼                                   ▼
      │                      │  ┌────────────┐  ┌────────────┐  ┌────────────┐
      │                      │  │  DIGITAL   │  │  CREDIT    │  │   BANK     │
      │                      │  │  WALLET    │  │  CARD      │  │  TRANSFER  │
      │                      │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
      │                      │        │               │               │
      │                      │        │               │               │
      │                      │   CONFIRMED       Validate         PENDING
      │                      │   (instant)       with API         _PAYMENT
      │                      │        │               │               │
      │                      │        │         ┌─────▼─────┐         │
      │                      │        │         │ External  │         │
      │                      │        │         │ Service   │         │
      │                      │        │         └─────┬─────┘         │
      │                      │        │               │               │
      │                      │        │         APPROVED?             │
      │                      │        │         ┌──┴──┐               │
      │                      │        │        Yes    No              │
      │                      │        │         │     │               │
      │                      │        │    CONFIRMED  REJECTED        │
      │                      │        │         │     │               │
      │<─────────────────────┼────────┴─────────┴─────┴───────────────┘
      │   BookingResponse    │
      │                      │
```

### Bank Transfer Payment Flow

```
┌──────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Bank      │     │    Kafka    │     │  Consumer   │     │  Booking    │
│   System     │     │             │     │             │     │  Service    │
└──────┬───────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                    │                   │                   │
       │ Payment Event      │                   │                   │
       │───────────────────>│                   │                   │
       │                    │                   │                   │
       │                    │  Consume Message  │                   │
       │                    │──────────────────>│                   │
       │                    │                   │                   │
       │                    │                   │ Check Duplicate   │
       │                    │                   │──────────────────>│
       │                    │                   │                   │
       │                    │                   │ Extract BookingId │
       │                    │                   │──────────────────>│
       │                    │                   │                   │
       │                    │                   │ Process Payment   │
       │                    │                   │──────────────────>│
       │                    │                   │                   │
       │                    │                   │     ┌─────────────┴──────────┐
       │                    │                   │     │ Full Payment Received? │
       │                    │                   │     └─────────────┬──────────┘
       │                    │                   │                   │
       │                    │                   │              ┌────┴────┐
       │                    │                   │             Yes       No
       │                    │                   │              │         │
       │                    │                   │         CONFIRMED  Keep PENDING
       │                    │                   │              │         │
       │                    │                   │<─────────────┴─────────┘
       │                    │                   │
       │                    │   ACK Message     │
       │                    │<──────────────────│
       │                    │                   │
```

---

## 🛠 Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.5.10 |
| **Build Tool** | Gradle | 8.x |
| **Database** | PostgreSQL | 15 |
| **Caching** | Redis | 7 |
| **Messaging** | Apache Kafka | 3.5+ |
| **Resilience** | Resilience4j | 2.2.0 |
| **Scheduling** | ShedLock | 5.10.0 |
| **API Docs** | SpringDoc OpenAPI | 2.8.4 |
| **Migrations** | Flyway | Latest |
| **Metrics** | Micrometer + Prometheus | Latest |
| **Tracing** | Zipkin (Brave) | Latest |
| **Testing** | JUnit 5, Testcontainers, Awaitility | Latest |
| **Containerization** | Docker & Docker Compose | Latest |

---

## 📋 Prerequisites

- **Java 21+** - [Download](https://adoptium.net/)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **Gradle 8.x** (optional - wrapper included)

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

Run the complete stack with all dependencies:

```bash
# Clone the repository
git clone <repository-url>
cd car-booking-service

# Navigate to docker folder and start all services
cd docker
docker-compose up --build -d

# View logs
docker-compose logs -f car-booking-service
```

**Services Started:**

| Service | URL | Description |
|---------|-----|-------------|
| Car Booking Service | http://localhost:8080 | Main application |
| Swagger UI | http://localhost:8080/swagger-ui.html | API Documentation |
| PostgreSQL | localhost:5432 | Database |
| Redis | localhost:6379 | Cache |
| Kafka | localhost:9092 | Message broker |
| Kafka UI | http://localhost:8090 | Kafka management |
| Mock Credit Card Service | http://localhost:9090 | WireMock stub |

### Option 2: Local Development

Run the application locally with Docker infrastructure:

```bash
# Start infrastructure services only
cd docker
docker-compose -f docker-compose-local.yml up -d

# Run the Spring Boot application (from project root)
cd ..
.\gradlew.bat bootRun
```

### Option 3: Minimal Setup (H2)

Run with in-memory database (requires Kafka):

```bash
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

---

## 📖 API Reference

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/bookings` | Create a new booking |
| `GET` | `/api/v1/bookings/{bookingId}` | Get booking by ID |
| `DELETE` | `/api/v1/bookings/{bookingId}` | Cancel a booking |

### Payment Modes

| Mode | Behavior | Status on Success |
|------|----------|-------------------|
| `DIGITAL_WALLET` | Instant confirmation | `CONFIRMED` |
| `CREDIT_CARD` | Validates with external service | `CONFIRMED` / `REJECTED` |
| `BANK_TRANSFER` | Async payment via Kafka | `PENDING_PAYMENT` → `CONFIRMED` |

### Vehicle Categories

| Category | Description |
|----------|-------------|
| `COMPACT` | Small economy cars |
| `SEDAN` | Mid-size sedans |
| `SUV` | Sport utility vehicles |
| `LUXURY` | Premium vehicles |

### Booking Statuses

| Status | Description |
|--------|-------------|
| `PENDING_PAYMENT` | Awaiting bank transfer payment |
| `CONFIRMED` | Booking confirmed and paid |
| `CANCELLED` | Booking cancelled |

### Request/Response Examples

**Create Booking Request:**
```json
POST /api/v1/bookings
Content-Type: application/json

{
  "customerName": "John Doe",
  "vehicleId": "VH-12345",
  "vehicleCategory": "SEDAN",
  "rentalStartDate": "2026-02-20",
  "rentalEndDate": "2026-02-25",
  "paymentMode": "DIGITAL_WALLET",
  "paymentReference": "PAY-REF-001"
}
```

**Response:**
```json
{
  "bookingId": "BKG0000001",
  "bookingStatus": "CONFIRMED"
}
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `local` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `car_booking_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `CREDIT_CARD_SERVICE_URL` | Credit card validation service URL | `http://localhost:9090` |

### Application Profiles

| Profile | Description | Cache Type |
|---------|-------------|------------|
| `local` | Local development | Simple (in-memory) |
| `dev` | Development with H2 | Simple |
| `docker` | Docker environment | Redis |
| `prod` | Production | Redis |
| `test` | Testing | Disabled |

---

## 🧪 Testing

### Running Tests

```bash
# Run unit tests
.\gradlew.bat test

# Run integration tests (requires Docker)
.\gradlew.bat integrationTest

# Run all tests with coverage
.\gradlew.bat test jacocoTestReport

# View coverage report
start build\reports\jacoco\test\html\index.html
```

### API Testing Examples

**Digital Wallet Payment (Instant):**
```bash
curl -X POST http://localhost:8080/api/v1/bookings ^
  -H "Content-Type: application/json" ^
  -d "{\"customerName\":\"John Doe\",\"vehicleId\":\"VH-001\",\"vehicleCategory\":\"SEDAN\",\"rentalStartDate\":\"2026-02-20\",\"rentalEndDate\":\"2026-02-25\",\"paymentMode\":\"DIGITAL_WALLET\",\"paymentReference\":\"WALLET-123\"}"
```

**Credit Card Payment:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings ^
  -H "Content-Type: application/json" ^
  -d "{\"customerName\":\"Jane Smith\",\"vehicleId\":\"VH-002\",\"vehicleCategory\":\"SUV\",\"rentalStartDate\":\"2026-02-20\",\"rentalEndDate\":\"2026-02-25\",\"paymentMode\":\"CREDIT_CARD\",\"paymentReference\":\"CC-APPROVED-123\"}"
```

**Bank Transfer Payment:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings ^
  -H "Content-Type: application/json" ^
  -d "{\"customerName\":\"Alice Brown\",\"vehicleId\":\"VH-003\",\"vehicleCategory\":\"COMPACT\",\"rentalStartDate\":\"2026-02-20\",\"rentalEndDate\":\"2026-02-25\",\"paymentMode\":\"BANK_TRANSFER\",\"paymentReference\":\"BT-REF-001\"}"
```

**Simulate Bank Transfer Payment (Kafka):**
```bash
# Connect to Kafka container
docker exec -it car-booking-kafka bash

# Send payment event
kafka-console-producer --broker-list localhost:9092 --topic bank-transfer-payment-events

# Paste JSON (replace BKG0000001 with actual booking ID):
{"paymentId":"PAY-001","senderAccountNumber":"NL91ABNA0417164300","paymentAmount":150.00,"transactionDetails":"TXN987654321 BKG0000001"}
```

---

## 📊 Observability

### Health Checks

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Overall health status |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |

### Metrics

| Endpoint | Description |
|----------|-------------|
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/caches` | Cache statistics |

**Custom Metrics:**

| Metric | Description |
|--------|-------------|
| `car_booking_bookings_created_total` | Total bookings created |
| `car_booking_bookings_confirmed_total` | Total bookings confirmed |
| `car_booking_bookings_cancelled_total` | Total bookings cancelled |
| `car_booking_payment_events_received_total` | Kafka events received |
| `car_booking_payment_events_processed_total` | Kafka events processed |

### Distributed Tracing

Enable Zipkin tracing:

```yaml
TRACING_ENABLED: true
ZIPKIN_ENDPOINT: http://zipkin:9411/api/v2/spans
```

---

## 🗄 Database Migrations

Flyway manages database schema migrations:

| Version | Description |
|---------|-------------|
| `V1` | Create booking tables |
| `V2` | Create ShedLock table |
| `V3` | Create processed payment events table |

Migrations run automatically on startup.

---

## 📁 Project Structure

```
car-booking-service/
├── src/
│   ├── main/
│   │   ├── java/com/velocity/carservice/
│   │   │   ├── application/           # Application layer
│   │   │   │   ├── dto/               # Request/Response DTOs
│   │   │   │   ├── service/           # Application services
│   │   │   │   └── strategy/          # Payment strategies
│   │   │   ├── config/                # Spring configurations
│   │   │   ├── domain/                # Domain layer
│   │   │   │   ├── model/             # Entities & enums
│   │   │   │   ├── repository/        # Repository interfaces
│   │   │   │   └── service/           # Domain services
│   │   │   ├── infrastructure/        # Infrastructure layer
│   │   │   │   ├── adapter/           # Adapters (inbound/outbound)
│   │   │   │   │   ├── inbound/       # REST, Kafka consumers
│   │   │   │   │   ├── outbound/      # External API clients
│   │   │   │   │   └── scheduler/     # Scheduled tasks
│   │   │   │   ├── exception/         # Exception handling
│   │   │   │   ├── filter/            # Request filters
│   │   │   │   ├── health/            # Health indicators
│   │   │   │   ├── metrics/           # Custom metrics
│   │   │   │   └── repository/        # JPA repositories
│   │   │   └── shared/                # Shared utilities
│   │   └── resources/
│   │       ├── application.yml        # Base configuration
│   │       ├── application-*.yml      # Profile configurations
│   │       └── db/migration/          # Flyway migrations
│   └── test/                          # Test classes
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml             # Full stack
│   ├── docker-compose-local.yml       # Infrastructure only
│   └── wiremock/                      # WireMock stubs
├── build.gradle.kts
└── README.md
```

---


## 📜 Business Rules

| Rule | Description |
|------|-------------|
| **Max Rental Duration** | Vehicle cannot be booked for more than 21 days |
| **Date Validation** | Rental end date must be after start date |
| **Auto-Cancellation** | Unpaid bank transfer bookings are cancelled 48 hours before rental start |
| **Idempotency** | Duplicate payment events are detected and ignored |

---


