CREATE TABLE joseph_auth.password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES joseph_auth.users(id) ON DELETE CASCADE,
    token       UUID         NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token ON joseph_auth.password_reset_tokens(token);
CREATE INDEX idx_password_reset_user ON joseph_auth.password_reset_tokens(user_id);
