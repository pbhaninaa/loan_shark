-- Demo users. Password values match the seeded Spring Boot migration.

INSERT INTO users (id, username, password, role, status, created_at)
VALUES
    (1, 'owner', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 'cashier', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'CASHIER', 'ACTIVE', CURRENT_TIMESTAMP),
    (3, 'borrower.demo', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', CURRENT_TIMESTAMP);
