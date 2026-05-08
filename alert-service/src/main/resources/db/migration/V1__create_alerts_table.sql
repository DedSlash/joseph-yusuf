CREATE SCHEMA IF NOT EXISTS joseph_alerts;

CREATE TABLE joseph_alerts.alerts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    type        VARCHAR(40)  NOT NULL,
    severity    VARCHAR(20)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    month       INT          CHECK (month IS NULL OR (month BETWEEN 1 AND 12)),
    year        INT          CHECK (year IS NULL OR year >= 2020),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_user_created ON joseph_alerts.alerts(user_id, created_at DESC);
CREATE INDEX idx_alerts_user_unread ON joseph_alerts.alerts(user_id, is_read) WHERE is_read = FALSE;
