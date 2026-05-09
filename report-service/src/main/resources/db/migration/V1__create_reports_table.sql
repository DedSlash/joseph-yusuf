CREATE SCHEMA IF NOT EXISTS joseph_reports;

CREATE TABLE joseph_reports.reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    month       INT          CHECK (month IS NULL OR month BETWEEN 1 AND 12),
    year        INT          NOT NULL CHECK (year >= 2020),
    file_name   VARCHAR(255) NOT NULL,
    pdf_content BYTEA        NOT NULL,
    size_bytes  INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_user ON joseph_reports.reports(user_id);
CREATE INDEX idx_reports_user_year_month ON joseph_reports.reports(user_id, year, month);
