CREATE SCHEMA IF NOT EXISTS joseph_income;

CREATE TABLE joseph_income.income_sources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'XOF',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE joseph_income.income_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    income_source_id UUID           NOT NULL REFERENCES joseph_income.income_sources(id) ON DELETE CASCADE,
    user_id          UUID           NOT NULL,
    amount           NUMERIC(15,2)  NOT NULL,
    month            INT            NOT NULL CHECK (month BETWEEN 1 AND 12),
    year             INT            NOT NULL CHECK (year >= 2020),
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_entry_source_month_year UNIQUE (income_source_id, month, year)
);

CREATE INDEX idx_income_sources_user ON joseph_income.income_sources(user_id);
CREATE INDEX idx_income_entries_user_month_year ON joseph_income.income_entries(user_id, month, year);
