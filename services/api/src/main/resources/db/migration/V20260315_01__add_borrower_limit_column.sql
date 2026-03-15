ALTER TABLE loan_interest_settings
    ADD COLUMN borrower_limit_percentage_previous_loan DECIMAL(5,2) DEFAULT 0.00;