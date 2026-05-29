CREATE TABLE IF NOT EXISTS joseph_auth.system_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO joseph_auth.system_settings (setting_key, setting_value, updated_at)
VALUES ('payments.active', 'false', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
