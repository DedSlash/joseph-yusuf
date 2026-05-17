ALTER TABLE joseph_income.income_entries
    ADD COLUMN amount_xof NUMERIC(15,2) NOT NULL DEFAULT 0;

-- Les entrées existantes sont supposées en XOF
UPDATE joseph_income.income_entries SET amount_xof = amount WHERE amount_xof = 0;
