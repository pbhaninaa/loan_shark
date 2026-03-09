-- Track total amount the owner has ever added from his pocket (only increases on top-up, never on repayments).
-- Lets the owner see "how much I put in" vs "how much I got back" at any time.
ALTER TABLE business_capital ADD COLUMN total_owner_added DECIMAL(14, 2) NOT NULL DEFAULT 0;

-- Backfill: for existing row, set total_owner_added = balance + disbursements - repayments (owner capital so far).
UPDATE business_capital bc
SET bc.total_owner_added = GREATEST(0,
    bc.balance
    + COALESCE((SELECT SUM(amount) FROM cash_transactions WHERE type = 'DISBURSEMENT'), 0)
    - COALESCE((SELECT SUM(amount) FROM cash_transactions WHERE type = 'REPAYMENT'), 0)
)
WHERE bc.id = '00000000-0000-0000-0000-000000000001';
