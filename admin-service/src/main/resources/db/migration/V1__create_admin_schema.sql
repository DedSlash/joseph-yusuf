CREATE SCHEMA IF NOT EXISTS joseph_admin;

CREATE TABLE joseph_admin.promo_codes (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(50)  NOT NULL UNIQUE,
    description       VARCHAR(255),
    discount_percent  INT          NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 100),
    max_uses          INT,
    used_count        INT          NOT NULL DEFAULT 0,
    expires_at        TIMESTAMPTZ,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by        UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promo_codes_code   ON joseph_admin.promo_codes(code);
CREATE INDEX idx_promo_codes_active ON joseph_admin.promo_codes(active);

CREATE TABLE joseph_admin.promo_code_usages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promo_code_id   UUID         NOT NULL REFERENCES joseph_admin.promo_codes(id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL,
    transaction_id  VARCHAR(100),
    used_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (promo_code_id, user_id)
);

CREATE INDEX idx_promo_usages_user   ON joseph_admin.promo_code_usages(user_id);
CREATE INDEX idx_promo_usages_promo  ON joseph_admin.promo_code_usages(promo_code_id);

CREATE TABLE joseph_admin.audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id     UUID         NOT NULL,
    action       VARCHAR(100) NOT NULL,
    target_type  VARCHAR(50),
    target_id    VARCHAR(100),
    details      TEXT,
    ip           VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_admin    ON joseph_admin.audit_logs(admin_id);
CREATE INDEX idx_audit_action   ON joseph_admin.audit_logs(action);
CREATE INDEX idx_audit_created  ON joseph_admin.audit_logs(created_at DESC);
