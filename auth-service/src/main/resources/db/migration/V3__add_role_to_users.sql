ALTER TABLE joseph_auth.users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX idx_users_role ON joseph_auth.users(role);
