ALTER TABLE joseph_auth.users
    ADD COLUMN IF NOT EXISTS country  VARCHAR(10) NOT NULL DEFAULT 'SN',
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'XOF';

CREATE INDEX IF NOT EXISTS idx_users_country ON joseph_auth.users(country);
