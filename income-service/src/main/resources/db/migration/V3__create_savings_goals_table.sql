-- Table joseph_income.savings_goals
-- @ExtractToSavingsService : à migrer vers joseph_savings.savings_goals post-lancement.
CREATE TABLE joseph_income.savings_goals (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    name                    VARCHAR(150)    NOT NULL,
    target_amount           NUMERIC(15,2)   NOT NULL CHECK (target_amount > 0),
    current_amount          NUMERIC(15,2)   NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
    monthly_target          NUMERIC(15,2)   CHECK (monthly_target IS NULL OR monthly_target >= 0),
    monthly_target_percent  NUMERIC(5,2)    CHECK (monthly_target_percent IS NULL OR (monthly_target_percent >= 0 AND monthly_target_percent <= 100)),
    start_date              DATE            NOT NULL,
    target_date             DATE,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_savings_goals_target_after_start
        CHECK (target_date IS NULL OR target_date >= start_date)
);

CREATE INDEX idx_savings_goals_user_active ON joseph_income.savings_goals(user_id, active);
CREATE INDEX idx_savings_goals_user_status ON joseph_income.savings_goals(user_id, status);
