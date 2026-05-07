CREATE SCHEMA IF NOT EXISTS joseph_auth;

CREATE TABLE joseph_auth.users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    plan        VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE joseph_auth.refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES joseph_auth.users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_refresh_tokens_token ON joseph_auth.refresh_tokens(token);
CREATE INDEX idx_users_email ON joseph_auth.users(email);
