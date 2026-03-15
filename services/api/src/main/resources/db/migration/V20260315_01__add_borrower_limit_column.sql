-- V20260315_01__add_borrower_limit_column.sql
-- Add borrower_limit_percentage_previous_loan column to loan_interest_settings table

ALTER TABLE loan_interest_settings
    ADD COLUMN borrower_limit_percentage_previous_loan DECIMAL(5,2) DEFAULT 0.00;borrower_limit_percentage_previous_loan DECIMAL(5,2) DEFAULT 0.00;