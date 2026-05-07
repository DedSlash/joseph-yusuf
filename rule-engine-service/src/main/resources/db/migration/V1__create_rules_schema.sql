CREATE SCHEMA IF NOT EXISTS joseph_rules;

CREATE TABLE joseph_rules.user_rule_configs (
    id                                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                           UUID        NOT NULL UNIQUE,
    active_rule                       VARCHAR(20) NOT NULL DEFAULT 'RULE_50_30_20',
    joseph_abundance_savings_percent  INT         NOT NULL DEFAULT 30,
    joseph_lean_savings_percent       INT         NOT NULL DEFAULT 10,
    created_at                        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_rule_configs_user ON joseph_rules.user_rule_configs(user_id);
