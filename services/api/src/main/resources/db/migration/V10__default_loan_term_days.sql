-- Default loan term (days) when borrower does not specify one; used for due date and interest calculation.
-- E.g. 365 = nominal 1-year term; actual payoff is determined by repayments.
ALTER TABLE loan_interest_settings ADD COLUMN default_loan_term_days INT NOT NULL DEFAULT 365;
