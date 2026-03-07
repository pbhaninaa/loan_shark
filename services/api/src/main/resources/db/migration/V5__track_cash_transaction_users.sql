ALTER TABLE cash_transactions
    ADD COLUMN captured_by BIGINT NULL AFTER reference_number,
    ADD COLUMN authorized_by BIGINT NULL AFTER captured_by;

UPDATE cash_transactions ct
LEFT JOIN repayments r
    ON r.loan_id = ct.loan_id
    AND r.reference_number = ct.reference_number
SET
    ct.captured_by = COALESCE(ct.captured_by, r.captured_by),
    ct.authorized_by = COALESCE(ct.authorized_by, r.captured_by)
WHERE ct.type = 'REPAYMENT';

UPDATE cash_transactions ct
JOIN loans l ON l.id = ct.loan_id
SET
    ct.captured_by = COALESCE(ct.captured_by, l.approved_by, l.created_by),
    ct.authorized_by = COALESCE(ct.authorized_by, l.approved_by, l.created_by)
WHERE ct.type = 'DISBURSEMENT';

ALTER TABLE cash_transactions
    MODIFY COLUMN captured_by BIGINT NOT NULL,
    MODIFY COLUMN authorized_by BIGINT NOT NULL,
    ADD CONSTRAINT fk_cash_captured_by FOREIGN KEY (captured_by) REFERENCES users(id),
    ADD CONSTRAINT fk_cash_authorized_by FOREIGN KEY (authorized_by) REFERENCES users(id);
