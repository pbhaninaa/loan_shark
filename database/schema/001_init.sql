-- Canonical schema for the Loan Shark platform.
-- Keep this aligned with `services/api/src/main/resources/db/migration/V1__init.sql`.

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE borrowers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    id_number VARCHAR(80) NOT NULL UNIQUE,
    phone VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(160) NULL,
    address VARCHAR(255) NOT NULL,
    employment_status VARCHAR(120) NOT NULL,
    monthly_income DECIMAL(12, 2) NOT NULL,
    employer_name VARCHAR(160) NULL,
    status VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE loans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    loan_amount DECIMAL(12, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    loan_term_days INT NOT NULL,
    issue_date DATE NULL,
    due_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    approved_by BIGINT NULL,
    risk_band VARCHAR(30) NOT NULL,
    risk_score INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE repayments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    amount_paid DECIMAL(12, 2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    captured_by BIGINT NOT NULL,
    reference_number VARCHAR(120) NOT NULL
);

CREATE TABLE repayment_schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    amount_due DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE blacklist_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    blacklisted_by BIGINT NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(120) NOT NULL,
    entity VARCHAR(120) NOT NULL,
    entity_id BIGINT NULL,
    details VARCHAR(2000) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE risk_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    loan_id BIGINT NOT NULL,
    score INT NOT NULL,
    band VARCHAR(30) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE cash_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_number VARCHAR(120) NOT NULL,
    captured_at TIMESTAMP NOT NULL
);
