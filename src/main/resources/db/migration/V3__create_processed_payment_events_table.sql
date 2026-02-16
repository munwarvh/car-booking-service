-- Create table for tracking processed Kafka payment events (idempotency/deduplication)
CREATE TABLE processed_payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id VARCHAR(100) NOT NULL UNIQUE,
    booking_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast duplicate lookups
CREATE INDEX idx_processed_payment_events_payment_id ON processed_payment_events(payment_id);

-- Index for querying by booking
CREATE INDEX idx_processed_payment_events_booking_id ON processed_payment_events(booking_id);

-- Index for querying by status (e.g., to find failed events)
CREATE INDEX idx_processed_payment_events_status ON processed_payment_events(status);

COMMENT ON TABLE processed_payment_events IS 'Tracks processed Kafka payment events for idempotency and audit';
COMMENT ON COLUMN processed_payment_events.payment_id IS 'Unique payment ID from Kafka event - used for deduplication';
COMMENT ON COLUMN processed_payment_events.status IS 'Processing status: SUCCESS, FAILED, SKIPPED, DUPLICATE';

