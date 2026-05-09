CREATE TABLE joseph_subscriptions.processed_webhook_events (
    event_id    VARCHAR(255) PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_type ON joseph_subscriptions.processed_webhook_events(event_type);
