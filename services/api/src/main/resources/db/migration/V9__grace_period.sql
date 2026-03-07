-- Grace period: days from disbursement when interest does not accumulate.
-- E.g. 2 days = if you were supposed to pay today and you didn't, you have 2 days to pay
-- before extra interest accumulates.
ALTER TABLE loan_interest_settings ADD COLUMN grace_period_days INT NOT NULL DEFAULT 0;
ALTER TABLE loans ADD COLUMN grace_period_days INT NOT NULL DEFAULT 0;
