ALTER TABLE borrower_verifications
    ADD COLUMN location_name VARCHAR(500) NULL AFTER location_captured_at;
