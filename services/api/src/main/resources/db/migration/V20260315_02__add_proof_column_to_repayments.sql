-- V20260315_02__add_proof_column_to_repayments.sql
ALTER TABLE repayments
    ADD COLUMN proof VARCHAR(255);