ALTER TABLE borrower_documents
    ADD COLUMN original_file_name VARCHAR(255) NOT NULL DEFAULT 'unknown',
    ADD COLUMN content_type VARCHAR(120) NOT NULL DEFAULT 'application/octet-stream',
    ADD COLUMN file_size_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN sha256_checksum VARCHAR(128) NOT NULL DEFAULT '';

CREATE TABLE borrower_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    id_document_id BIGINT NOT NULL,
    selfie_document_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    location_captured_at TIMESTAMP NULL,
    sa_id_valid BIT NOT NULL,
    ocr_confidence DECIMAL(5, 2) NULL,
    extracted_first_name VARCHAR(120) NULL,
    extracted_last_name VARCHAR(120) NULL,
    extracted_id_number VARCHAR(80) NULL,
    details_matched BIT NOT NULL,
    face_match_score DECIMAL(5, 2) NULL,
    face_matched BIT NOT NULL,
    review_notes VARCHAR(2000) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_verification_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers(id),
    CONSTRAINT fk_verification_id_document FOREIGN KEY (id_document_id) REFERENCES borrower_documents(id),
    CONSTRAINT fk_verification_selfie_document FOREIGN KEY (selfie_document_id) REFERENCES borrower_documents(id),
    CONSTRAINT fk_verification_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE INDEX idx_verification_borrower ON borrower_verifications(borrower_id);
CREATE INDEX idx_verification_status ON borrower_verifications(status);
