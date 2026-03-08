-- Max loan amount as percentage of borrower's monthly salary (e.g. 25 = client can borrow up to 25% of monthly income).
ALTER TABLE loan_interest_settings ADD COLUMN borrower_limit_percentage DECIMAL(5, 2) NOT NULL DEFAULT 100.00;
