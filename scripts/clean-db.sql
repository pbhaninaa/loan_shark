-- =============================================================================
-- Loan Shark: Clean database for fresh testing
-- =============================================================================
-- Run this in MySQL Workbench (or mysql CLI) against the loan_shark database.
-- This removes ALL data but keeps the schema (tables, indexes, Flyway history).
-- After running:
--   1. Start the API; no migrations will re-run.
--   2. Open the app: you will see "Create owner account" (no users exist).
--   3. Create your owner account and test everything from scratch.
-- =============================================================================

USE loan_shark;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE repayments;
TRUNCATE TABLE repayment_schedules;
TRUNCATE TABLE risk_assessments;
TRUNCATE TABLE blacklist_entries;
TRUNCATE TABLE cash_transactions;
TRUNCATE TABLE loans;
TRUNCATE TABLE borrower_verifications;
TRUNCATE TABLE borrower_documents;
TRUNCATE TABLE borrowers;
TRUNCATE TABLE notifications;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE password_reset_tokens;
TRUNCATE TABLE users;
TRUNCATE TABLE loan_interest_settings;
TRUNCATE TABLE business_capital;

SET FOREIGN_KEY_CHECKS = 1;

-- Re-insert required single-row data so the app works without re-running migrations.
INSERT INTO loan_interest_settings (
    id,
    default_interest_rate,
    interest_type,
    interest_period_days,
    grace_period_days,
    default_loan_term_days,
    updated_at
) VALUES (
    1,
    30.00,
    'SIMPLE',
    30,
    0,
    365,
    CURRENT_TIMESTAMP
);

INSERT INTO business_capital (id, balance, updated_at) VALUES (1, 0, CURRENT_TIMESTAMP);

SELECT 'Database cleaned. Create owner account on next login.' AS message;
