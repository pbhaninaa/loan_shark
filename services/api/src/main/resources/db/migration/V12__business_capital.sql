-- Single row: available lending pool (initial + top-ups - disbursements + repayments)
CREATE TABLE business_capital (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    balance DECIMAL(14, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO business_capital (id, balance, updated_at) VALUES (1, 0, CURRENT_TIMESTAMP);
