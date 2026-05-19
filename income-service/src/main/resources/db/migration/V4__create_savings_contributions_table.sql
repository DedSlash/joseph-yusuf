-- Table joseph_income.savings_contributions
-- @ExtractToSavingsService : à migrer vers joseph_savings.savings_contributions post-lancement.
CREATE TABLE joseph_income.savings_contributions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID            NOT NULL REFERENCES joseph_income.savings_goals(id) ON DELETE CASCADE,
    user_id         UUID            NOT NULL,
    amount          NUMERIC(15,2)   NOT NULL CHECK (amount > 0),
    month           INT             NOT NULL CHECK (month BETWEEN 1 AND 12),
    year            INT             NOT NULL CHECK (year >= 2020),
    type            VARCHAR(20)     NOT NULL,
    joseph_status   VARCHAR(20),
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_savings_contributions_goal ON joseph_income.savings_contributions(goal_id, year DESC, month DESC);
CREATE INDEX idx_savings_contributions_user ON joseph_income.savings_contributions(user_id, year DESC, month DESC);
