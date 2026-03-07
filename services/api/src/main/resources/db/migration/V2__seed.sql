INSERT INTO users (id, username, password, role, status, created_at)
VALUES
    (1, 'owner', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 'cashier', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'CASHIER', 'ACTIVE', CURRENT_TIMESTAMP),
    (3, 'borrower.demo', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO borrowers (
    id, user_id, first_name, last_name, id_number, phone, email, address, employment_status,
    monthly_income, employer_name, status, risk_score, created_at
)
VALUES (
    1, 3, 'Demo', 'Borrower', '9001015009087', '+27820000001', 'demo@example.com',
    '12 Main Street', 'EMPLOYED', 8000.00, 'Corner Shop', 'ACTIVE', 10, CURRENT_TIMESTAMP
);

INSERT INTO notifications (user_id, channel, message, status, created_at)
VALUES
    (3, 'PUSH', 'Welcome to Loan Shark. Complete your first application to get started.', 'PENDING', CURRENT_TIMESTAMP),
    (1, 'SYSTEM', 'Daily dashboard is ready.', 'PENDING', CURRENT_TIMESTAMP);
