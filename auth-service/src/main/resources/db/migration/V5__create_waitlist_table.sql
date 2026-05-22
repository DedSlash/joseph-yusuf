CREATE TABLE joseph_auth.waitlist (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(150) NOT NULL,
    plan_tier           VARCHAR(20)  NOT NULL,
    country             VARCHAR(10)  NOT NULL DEFAULT 'SN',
    currency            VARCHAR(10)  NOT NULL DEFAULT 'XOF',
    promo_code_reserved VARCHAR(50),
    notified            BOOLEAN      NOT NULL DEFAULT FALSE,
    notified_at         TIMESTAMPTZ,
    converted_user_id   UUID REFERENCES joseph_auth.users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_waitlist_email_plan ON joseph_auth.waitlist(email, plan_tier);
CREATE INDEX idx_waitlist_notified ON joseph_auth.waitlist(notified);
CREATE INDEX idx_waitlist_plan_tier ON joseph_auth.waitlist(plan_tier);
