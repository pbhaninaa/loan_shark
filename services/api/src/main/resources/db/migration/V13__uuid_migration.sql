-- =============================================================================
-- V13: Migrate all primary and foreign keys from BIGINT to UUID (CHAR(36))
-- Run against existing DB; preserves data by mapping old ids to new UUIDs.
-- =============================================================================

SET @fk_checks = @@foreign_key_checks;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. USERS
-- -----------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE users SET id_uuid = LOWER(UUID());

CREATE TEMPORARY TABLE users_map (old_id BIGINT PRIMARY KEY, new_id CHAR(36));
INSERT INTO users_map (old_id, new_id) SELECT id, id_uuid FROM users;

ALTER TABLE borrowers ADD COLUMN user_id_uuid CHAR(36) NULL;
UPDATE borrowers b INNER JOIN users_map m ON b.user_id = m.old_id SET b.user_id_uuid = m.new_id;

ALTER TABLE audit_logs ADD COLUMN user_id_uuid CHAR(36) NULL;
UPDATE audit_logs a INNER JOIN users_map m ON a.user_id = m.old_id SET a.user_id_uuid = m.new_id;

ALTER TABLE notifications ADD COLUMN user_id_uuid CHAR(36) NULL;
UPDATE notifications n INNER JOIN users_map m ON n.user_id = m.old_id SET n.user_id_uuid = m.new_id;

-- Drop FKs referencing users
ALTER TABLE borrowers DROP FOREIGN KEY fk_borrower_user;
ALTER TABLE loans DROP FOREIGN KEY fk_loan_created_by;
ALTER TABLE loans DROP FOREIGN KEY fk_loan_approved_by;
ALTER TABLE repayments DROP FOREIGN KEY fk_repayment_captured_by;
ALTER TABLE blacklist_entries DROP FOREIGN KEY fk_blacklist_user;
ALTER TABLE cash_transactions DROP FOREIGN KEY fk_cash_captured_by;
ALTER TABLE cash_transactions DROP FOREIGN KEY fk_cash_authorized_by;
ALTER TABLE borrower_verifications DROP FOREIGN KEY fk_verification_reviewed_by;
ALTER TABLE password_reset_tokens DROP FOREIGN KEY fk_reset_token_user;

ALTER TABLE borrowers DROP COLUMN user_id, CHANGE COLUMN user_id_uuid user_id CHAR(36) NULL;
ALTER TABLE audit_logs DROP COLUMN user_id, CHANGE COLUMN user_id_uuid user_id CHAR(36) NULL;
ALTER TABLE notifications DROP COLUMN user_id, CHANGE COLUMN user_id_uuid user_id CHAR(36) NULL;

ALTER TABLE users DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

-- Re-add FKs to users (after users has new PK)
ALTER TABLE borrowers ADD CONSTRAINT fk_borrower_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id);

-- loans, repayments, blacklist, cash_transactions, borrower_verifications, password_reset_tokens need to be updated after we have users.id. We already dropped their FKs. We'll add them back after converting those tables. For now we need to convert loans (created_by, approved_by) - we need users_map. So we keep users_map until we've updated all tables. So the order is: convert users (done), then borrowers, then borrower_documents, loans, etc. And we need users_map when we update loans.created_by, loans.approved_by, repayments.captured_by, etc. So let's keep users_map and continue.

ALTER TABLE loans ADD COLUMN created_by_uuid CHAR(36) NULL, ADD COLUMN approved_by_uuid CHAR(36) NULL;
UPDATE loans l INNER JOIN users_map m ON l.created_by = m.old_id SET l.created_by_uuid = m.new_id;
UPDATE loans l INNER JOIN users_map m ON l.approved_by = m.old_id SET l.approved_by_uuid = m.new_id;
ALTER TABLE loans DROP COLUMN created_by, DROP COLUMN approved_by, CHANGE COLUMN created_by_uuid created_by CHAR(36) NOT NULL, CHANGE COLUMN approved_by_uuid approved_by CHAR(36) NULL;

ALTER TABLE repayments ADD COLUMN captured_by_uuid CHAR(36) NULL;
UPDATE repayments r INNER JOIN users_map m ON r.captured_by = m.old_id SET r.captured_by_uuid = m.new_id;
ALTER TABLE repayments DROP COLUMN captured_by, CHANGE COLUMN captured_by_uuid captured_by CHAR(36) NOT NULL;

ALTER TABLE blacklist_entries ADD COLUMN blacklisted_by_uuid CHAR(36) NULL;
UPDATE blacklist_entries b INNER JOIN users_map m ON b.blacklisted_by = m.old_id SET b.blacklisted_by_uuid = m.new_id;
ALTER TABLE blacklist_entries DROP COLUMN blacklisted_by, CHANGE COLUMN blacklisted_by_uuid blacklisted_by CHAR(36) NOT NULL;

ALTER TABLE cash_transactions ADD COLUMN captured_by_uuid CHAR(36) NULL, ADD COLUMN authorized_by_uuid CHAR(36) NULL;
UPDATE cash_transactions c INNER JOIN users_map m ON c.captured_by = m.old_id SET c.captured_by_uuid = m.new_id;
UPDATE cash_transactions c INNER JOIN users_map m ON c.authorized_by = m.old_id SET c.authorized_by_uuid = m.new_id;
ALTER TABLE cash_transactions DROP COLUMN captured_by, DROP COLUMN authorized_by, CHANGE COLUMN captured_by_uuid captured_by CHAR(36) NOT NULL, CHANGE COLUMN authorized_by_uuid authorized_by CHAR(36) NOT NULL;

ALTER TABLE borrower_verifications ADD COLUMN reviewed_by_uuid CHAR(36) NULL;
UPDATE borrower_verifications v INNER JOIN users_map m ON v.reviewed_by = m.old_id SET v.reviewed_by_uuid = m.new_id;
ALTER TABLE borrower_verifications DROP COLUMN reviewed_by, CHANGE COLUMN reviewed_by_uuid reviewed_by CHAR(36) NULL;

ALTER TABLE password_reset_tokens ADD COLUMN user_id_uuid CHAR(36) NULL;
UPDATE password_reset_tokens p INNER JOIN users_map m ON p.user_id = m.old_id SET p.user_id_uuid = m.new_id;
ALTER TABLE password_reset_tokens DROP COLUMN user_id, CHANGE COLUMN user_id_uuid user_id CHAR(36) NOT NULL;

ALTER TABLE loans ADD CONSTRAINT fk_loan_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE loans ADD CONSTRAINT fk_loan_approved_by FOREIGN KEY (approved_by) REFERENCES users(id);
ALTER TABLE repayments ADD CONSTRAINT fk_repayment_captured_by FOREIGN KEY (captured_by) REFERENCES users(id);
ALTER TABLE blacklist_entries ADD CONSTRAINT fk_blacklist_user FOREIGN KEY (blacklisted_by) REFERENCES users(id);
ALTER TABLE cash_transactions ADD CONSTRAINT fk_cash_captured_by FOREIGN KEY (captured_by) REFERENCES users(id);
ALTER TABLE cash_transactions ADD CONSTRAINT fk_cash_authorized_by FOREIGN KEY (authorized_by) REFERENCES users(id);
ALTER TABLE borrower_verifications ADD CONSTRAINT fk_verification_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id);
ALTER TABLE password_reset_tokens ADD CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

DROP TEMPORARY TABLE users_map;

-- -----------------------------------------------------------------------------
-- 2. BORROWERS
-- -----------------------------------------------------------------------------
ALTER TABLE borrowers ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE borrowers SET id_uuid = LOWER(UUID());
CREATE TEMPORARY TABLE borrowers_map (old_id BIGINT PRIMARY KEY, new_id CHAR(36));
INSERT INTO borrowers_map (old_id, new_id) SELECT id, id_uuid FROM borrowers;

ALTER TABLE borrower_documents ADD COLUMN borrower_id_uuid CHAR(36) NULL;
UPDATE borrower_documents d INNER JOIN borrowers_map m ON d.borrower_id = m.old_id SET d.borrower_id_uuid = m.new_id;
ALTER TABLE loans ADD COLUMN borrower_id_uuid CHAR(36) NULL;
UPDATE loans l INNER JOIN borrowers_map m ON l.borrower_id = m.old_id SET l.borrower_id_uuid = m.new_id;
ALTER TABLE blacklist_entries ADD COLUMN borrower_id_uuid CHAR(36) NULL;
UPDATE blacklist_entries b INNER JOIN borrowers_map m ON b.borrower_id = m.old_id SET b.borrower_id_uuid = m.new_id;
ALTER TABLE risk_assessments ADD COLUMN borrower_id_uuid CHAR(36) NULL;
UPDATE risk_assessments r INNER JOIN borrowers_map m ON r.borrower_id = m.old_id SET r.borrower_id_uuid = m.new_id;
ALTER TABLE borrower_verifications ADD COLUMN borrower_id_uuid CHAR(36) NULL;
UPDATE borrower_verifications v INNER JOIN borrowers_map m ON v.borrower_id = m.old_id SET v.borrower_id_uuid = m.new_id;

ALTER TABLE borrower_documents DROP FOREIGN KEY fk_document_borrower;
ALTER TABLE loans DROP FOREIGN KEY fk_loan_borrower;
ALTER TABLE blacklist_entries DROP FOREIGN KEY fk_blacklist_borrower;
ALTER TABLE risk_assessments DROP FOREIGN KEY fk_risk_borrower;
ALTER TABLE borrower_verifications DROP FOREIGN KEY fk_verification_borrower;

ALTER TABLE borrower_documents DROP COLUMN borrower_id, CHANGE COLUMN borrower_id_uuid borrower_id CHAR(36) NOT NULL;
ALTER TABLE loans DROP COLUMN borrower_id, CHANGE COLUMN borrower_id_uuid borrower_id CHAR(36) NOT NULL;
ALTER TABLE blacklist_entries DROP COLUMN borrower_id, CHANGE COLUMN borrower_id_uuid borrower_id CHAR(36) NOT NULL;
ALTER TABLE risk_assessments DROP COLUMN borrower_id, CHANGE COLUMN borrower_id_uuid borrower_id CHAR(36) NOT NULL;
ALTER TABLE borrower_verifications DROP COLUMN borrower_id, CHANGE COLUMN borrower_id_uuid borrower_id CHAR(36) NOT NULL;

ALTER TABLE borrowers DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);
ALTER TABLE borrower_documents ADD CONSTRAINT fk_document_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id);
ALTER TABLE loans ADD CONSTRAINT fk_loan_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id);
ALTER TABLE blacklist_entries ADD CONSTRAINT fk_blacklist_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id);
ALTER TABLE risk_assessments ADD CONSTRAINT fk_risk_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id);
ALTER TABLE borrower_verifications ADD CONSTRAINT fk_verification_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id);
DROP TEMPORARY TABLE borrowers_map;

-- -----------------------------------------------------------------------------
-- 3. BORROWER_DOCUMENTS
-- -----------------------------------------------------------------------------
ALTER TABLE borrower_documents ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE borrower_documents SET id_uuid = LOWER(UUID());
CREATE TEMPORARY TABLE borrower_documents_map (old_id BIGINT PRIMARY KEY, new_id CHAR(36));
INSERT INTO borrower_documents_map (old_id, new_id) SELECT id, id_uuid FROM borrower_documents;

ALTER TABLE borrower_verifications ADD COLUMN id_document_id_uuid CHAR(36) NULL, ADD COLUMN selfie_document_id_uuid CHAR(36) NULL;
UPDATE borrower_verifications v INNER JOIN borrower_documents_map m ON v.id_document_id = m.old_id SET v.id_document_id_uuid = m.new_id;
UPDATE borrower_verifications v INNER JOIN borrower_documents_map m ON v.selfie_document_id = m.old_id SET v.selfie_document_id_uuid = m.new_id;

ALTER TABLE borrower_verifications DROP FOREIGN KEY fk_verification_id_document;
ALTER TABLE borrower_verifications DROP FOREIGN KEY fk_verification_selfie_document;
ALTER TABLE borrower_verifications DROP COLUMN id_document_id, DROP COLUMN selfie_document_id;
ALTER TABLE borrower_verifications CHANGE COLUMN id_document_id_uuid id_document_id CHAR(36) NOT NULL, CHANGE COLUMN selfie_document_id_uuid selfie_document_id CHAR(36) NOT NULL;

ALTER TABLE borrower_documents DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);
ALTER TABLE borrower_verifications ADD CONSTRAINT fk_verification_id_document FOREIGN KEY (id_document_id) REFERENCES borrower_documents(id);
ALTER TABLE borrower_verifications ADD CONSTRAINT fk_verification_selfie_document FOREIGN KEY (selfie_document_id) REFERENCES borrower_documents(id);
DROP TEMPORARY TABLE borrower_documents_map;

-- -----------------------------------------------------------------------------
-- 4. LOANS
-- -----------------------------------------------------------------------------
ALTER TABLE loans ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE loans SET id_uuid = LOWER(UUID());
CREATE TEMPORARY TABLE loans_map (old_id BIGINT PRIMARY KEY, new_id CHAR(36));
INSERT INTO loans_map (old_id, new_id) SELECT id, id_uuid FROM loans;

ALTER TABLE repayments ADD COLUMN loan_id_uuid CHAR(36) NULL;
UPDATE repayments r INNER JOIN loans_map m ON r.loan_id = m.old_id SET r.loan_id_uuid = m.new_id;
ALTER TABLE repayment_schedules ADD COLUMN loan_id_uuid CHAR(36) NULL;
UPDATE repayment_schedules s INNER JOIN loans_map m ON s.loan_id = m.old_id SET s.loan_id_uuid = m.new_id;
ALTER TABLE risk_assessments ADD COLUMN loan_id_uuid CHAR(36) NULL;
UPDATE risk_assessments r INNER JOIN loans_map m ON r.loan_id = m.old_id SET r.loan_id_uuid = m.new_id;
ALTER TABLE cash_transactions ADD COLUMN loan_id_uuid CHAR(36) NULL;
UPDATE cash_transactions c INNER JOIN loans_map m ON c.loan_id = m.old_id SET c.loan_id_uuid = m.new_id;

ALTER TABLE repayments DROP FOREIGN KEY fk_repayment_loan;
ALTER TABLE repayment_schedules DROP FOREIGN KEY fk_schedule_loan;
ALTER TABLE risk_assessments DROP FOREIGN KEY fk_risk_loan;
ALTER TABLE cash_transactions DROP FOREIGN KEY fk_cash_loan;

ALTER TABLE repayments DROP COLUMN loan_id, CHANGE COLUMN loan_id_uuid loan_id CHAR(36) NOT NULL;
ALTER TABLE repayment_schedules DROP COLUMN loan_id, CHANGE COLUMN loan_id_uuid loan_id CHAR(36) NOT NULL;
ALTER TABLE risk_assessments DROP COLUMN loan_id, CHANGE COLUMN loan_id_uuid loan_id CHAR(36) NOT NULL;
ALTER TABLE cash_transactions DROP COLUMN loan_id, CHANGE COLUMN loan_id_uuid loan_id CHAR(36) NULL;

ALTER TABLE loans DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);
ALTER TABLE repayments ADD CONSTRAINT fk_repayment_loan FOREIGN KEY (loan_id) REFERENCES loans(id);
ALTER TABLE repayment_schedules ADD CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loans(id);
ALTER TABLE risk_assessments ADD CONSTRAINT fk_risk_loan FOREIGN KEY (loan_id) REFERENCES loans(id);
ALTER TABLE cash_transactions ADD CONSTRAINT fk_cash_loan FOREIGN KEY (loan_id) REFERENCES loans(id);
DROP TEMPORARY TABLE loans_map;

-- -----------------------------------------------------------------------------
-- 5. REMAINING TABLES (no FKs to other unconverted tables)
-- -----------------------------------------------------------------------------
ALTER TABLE repayments ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE repayments SET id_uuid = LOWER(UUID());
ALTER TABLE repayments DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE repayment_schedules ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE repayment_schedules SET id_uuid = LOWER(UUID());
ALTER TABLE repayment_schedules DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE blacklist_entries ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE blacklist_entries SET id_uuid = LOWER(UUID());
ALTER TABLE blacklist_entries DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE audit_logs ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE audit_logs SET id_uuid = LOWER(UUID());
ALTER TABLE audit_logs DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);
ALTER TABLE audit_logs MODIFY COLUMN entity_id VARCHAR(36) NULL;

ALTER TABLE risk_assessments ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE risk_assessments SET id_uuid = LOWER(UUID());
ALTER TABLE risk_assessments DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE notifications ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE notifications SET id_uuid = LOWER(UUID());
ALTER TABLE notifications DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE cash_transactions ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE cash_transactions SET id_uuid = LOWER(UUID());
ALTER TABLE cash_transactions DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE borrower_verifications ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE borrower_verifications SET id_uuid = LOWER(UUID());
ALTER TABLE borrower_verifications DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE password_reset_tokens ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE password_reset_tokens SET id_uuid = LOWER(UUID());
ALTER TABLE password_reset_tokens DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

-- Fixed UUIDs for single-row config tables so app can find them
ALTER TABLE business_capital ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE business_capital SET id_uuid = '00000000-0000-0000-0000-000000000001' WHERE id = 1;
ALTER TABLE business_capital DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

ALTER TABLE loan_interest_settings ADD COLUMN id_uuid CHAR(36) NULL;
UPDATE loan_interest_settings SET id_uuid = '00000000-0000-0000-0000-000000000002' WHERE id = 1;
ALTER TABLE loan_interest_settings DROP PRIMARY KEY, DROP COLUMN id, CHANGE COLUMN id_uuid id CHAR(36) NOT NULL, ADD PRIMARY KEY (id);

SET FOREIGN_KEY_CHECKS = @fk_checks;
