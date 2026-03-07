-- Admin-configurable interest: rate, simple/compound, period length (e.g. 30 days).
-- Interest accrues from disbursement; first period starts when borrower gets the money;
-- after each period (e.g. 30 days) another interest period accumulates.
CREATE TABLE loan_interest_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    default_interest_rate DECIMAL(5, 2) NOT NULL,
    interest_type VARCHAR(20) NOT NULL,
    interest_period_days INT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_interest_type CHECK (interest_type IN ('SIMPLE', 'COMPOUND')),
    CONSTRAINT chk_interest_period CHECK (interest_period_days > 0)
);

INSERT INTO loan_interest_settings (default_interest_rate, interest_type, interest_period_days, updated_at)
VALUES (30.00, 'SIMPLE', 30, CURRENT_TIMESTAMP);

-- Store which settings were used per loan for audit and display
ALTER TABLE loans ADD COLUMN interest_type VARCHAR(20) NULL;
ALTER TABLE loans ADD COLUMN interest_period_days INT NULL;

UPDATE loans SET interest_type = 'SIMPLE', interest_period_days = 30 WHERE interest_type IS NULL;

ALTER TABLE loans MODIFY COLUMN interest_type VARCHAR(20) NOT NULL;
ALTER TABLE loans MODIFY COLUMN interest_period_days INT NOT NULL;
