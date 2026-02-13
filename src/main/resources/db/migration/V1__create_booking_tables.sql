-- V1__create_booking_tables.sql
-- Car Booking Service Database Schema for Velocity Motors

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY,
    booking_id VARCHAR(10) UNIQUE NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    vehicle_id VARCHAR(50) NOT NULL,
    vehicle_category VARCHAR(20) NOT NULL,
    rental_start_date DATE NOT NULL,
    rental_end_date DATE NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    payment_reference VARCHAR(100),
    payment_amount DECIMAL(10, 2),
    amount_received DECIMAL(10, 2) DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_bookings_booking_id ON bookings(booking_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_mode_status ON bookings(payment_mode, status);
CREATE INDEX IF NOT EXISTS idx_bookings_rental_start_date ON bookings(rental_start_date);
