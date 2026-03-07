-- Only seed demo data when an owner already exists (e.g. V2 was not followed by V3, or owner was re-created).
-- After V3, users table is empty so we skip all demo inserts and allow "Create owner account" on first run.
SET @owner_id = (SELECT id FROM users WHERE role = 'OWNER' ORDER BY id LIMIT 1);
SET @operator_id = COALESCE((SELECT id FROM users WHERE role = 'CASHIER' ORDER BY id LIMIT 1), @owner_id);

INSERT INTO users (username, password, role, status, created_at)
SELECT 'demo.borrower.one', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo.borrower.one') AND @owner_id IS NOT NULL;

INSERT INTO users (username, password, role, status, created_at)
SELECT 'demo.borrower.two', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo.borrower.two') AND @owner_id IS NOT NULL;

INSERT INTO users (username, password, role, status, created_at)
SELECT 'demo.borrower.three', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo.borrower.three') AND @owner_id IS NOT NULL;

INSERT INTO users (username, password, role, status, created_at)
SELECT 'demo.borrower.four', '$2b$10$MMubIZjIpzdfroJJ7T5yE./oRolvd1aLLiSxa/Z6yKWgccNJpaOIS', 'BORROWER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo.borrower.four') AND @owner_id IS NOT NULL;

INSERT INTO borrowers (
    user_id, first_name, last_name, id_number, phone, email, address,
    employment_status, monthly_income, employer_name, status, risk_score, created_at
)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.one'),
    'Naledi', 'Dlamini', '9501015009081', '+27820000011', 'naledi@example.com', '14 Palm Street, Soweto',
    'EMPLOYED', 8500.00, 'Fresh Mart', 'ACTIVE', 22, NOW()
WHERE NOT EXISTS (SELECT 1 FROM borrowers WHERE id_number = '9501015009081') AND @owner_id IS NOT NULL;

INSERT INTO borrowers (
    user_id, first_name, last_name, id_number, phone, email, address,
    employment_status, monthly_income, employer_name, status, risk_score, created_at
)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.two'),
    'Thabo', 'Mokoena', '9102025009082', '+27820000012', 'thabo@example.com', '28 Freedom Road, Alexandra',
    'SELF_EMPLOYED', 6200.00, 'Township Repairs', 'ACTIVE', 48, NOW()
WHERE NOT EXISTS (SELECT 1 FROM borrowers WHERE id_number = '9102025009082') AND @owner_id IS NOT NULL;

INSERT INTO borrowers (
    user_id, first_name, last_name, id_number, phone, email, address,
    employment_status, monthly_income, employer_name, status, risk_score, created_at
)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.three'),
    'Ayanda', 'Khumalo', '8903035009083', '+27820000013', 'ayanda@example.com', '55 Market Lane, Durban',
    'EMPLOYED', 11000.00, 'Metro Foods', 'ACTIVE', 18, NOW()
WHERE NOT EXISTS (SELECT 1 FROM borrowers WHERE id_number = '8903035009083') AND @owner_id IS NOT NULL;

INSERT INTO borrowers (
    user_id, first_name, last_name, id_number, phone, email, address,
    employment_status, monthly_income, employer_name, status, risk_score, created_at
)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.four'),
    'Lerato', 'Ncube', '8704045009084', '+27820000014', 'lerato@example.com', '10 Hillview, Pretoria',
    'UNEMPLOYED', 2500.00, NULL, 'BLACKLISTED', 82, NOW()
WHERE NOT EXISTS (SELECT 1 FROM borrowers WHERE id_number = '8704045009084') AND @owner_id IS NOT NULL;

INSERT INTO borrower_documents (borrower_id, document_type, file_url, uploaded_at)
SELECT (SELECT id FROM borrowers WHERE id_number = '9501015009081'), 'ID_COPY', 'https://example.com/docs/naledi-id.pdf', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM borrower_documents
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081')
      AND document_type = 'ID_COPY'
) AND @owner_id IS NOT NULL;

INSERT INTO borrower_documents (borrower_id, document_type, file_url, uploaded_at)
SELECT (SELECT id FROM borrowers WHERE id_number = '9501015009081'), 'PAYSLIP', 'https://example.com/docs/naledi-payslip.pdf', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM borrower_documents
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081')
      AND document_type = 'PAYSLIP'
) AND @owner_id IS NOT NULL;

INSERT INTO borrower_documents (borrower_id, document_type, file_url, uploaded_at)
SELECT (SELECT id FROM borrowers WHERE id_number = '9102025009082'), 'BANK_STATEMENT', 'https://example.com/docs/thabo-bank.pdf', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM borrower_documents
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9102025009082')
      AND document_type = 'BANK_STATEMENT'
) AND @owner_id IS NOT NULL;

INSERT INTO borrower_documents (borrower_id, document_type, file_url, uploaded_at)
SELECT (SELECT id FROM borrowers WHERE id_number = '8903035009083'), 'PROOF_OF_ADDRESS', 'https://example.com/docs/ayanda-address.pdf', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM borrower_documents
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083')
      AND document_type = 'PROOF_OF_ADDRESS'
) AND @owner_id IS NOT NULL;

INSERT INTO loans (
    borrower_id, loan_amount, interest_rate, total_amount, loan_term_days,
    issue_date, due_date, status, created_by, approved_by, risk_band, risk_score, created_at
)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '9501015009081'),
    2500.00, 30.00, 3250.00, 30,
    CURRENT_DATE - INTERVAL 14 DAY, CURRENT_DATE + INTERVAL 16 DAY,
    'ACTIVE', @operator_id, @owner_id, 'SAFE', 22, NOW()
FROM (SELECT 1 AS d WHERE @owner_id IS NOT NULL AND @operator_id IS NOT NULL) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM loans
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081')
      AND status = 'ACTIVE'
);

INSERT INTO loans (
    borrower_id, loan_amount, interest_rate, total_amount, loan_term_days,
    issue_date, due_date, status, created_by, approved_by, risk_band, risk_score, created_at
)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '9102025009082'),
    1800.00, 25.00, 2250.00, 21,
    NULL, NULL,
    'PENDING', @operator_id, NULL, 'MEDIUM_RISK', 48, NOW()
FROM (SELECT 1 AS d WHERE @operator_id IS NOT NULL) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM loans
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9102025009082')
      AND status = 'PENDING'
);

INSERT INTO loans (
    borrower_id, loan_amount, interest_rate, total_amount, loan_term_days,
    issue_date, due_date, status, created_by, approved_by, risk_band, risk_score, created_at
)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '8903035009083'),
    1200.00, 20.00, 1440.00, 28,
    CURRENT_DATE - INTERVAL 42 DAY, CURRENT_DATE - INTERVAL 14 DAY,
    'COMPLETED', @operator_id, @owner_id, 'SAFE', 18, NOW()
FROM (SELECT 1 AS d WHERE @owner_id IS NOT NULL AND @operator_id IS NOT NULL) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM loans
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083')
      AND status = 'COMPLETED'
);

INSERT INTO loans (
    borrower_id, loan_amount, interest_rate, total_amount, loan_term_days,
    issue_date, due_date, status, created_by, approved_by, risk_band, risk_score, created_at
)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '8704045009084'),
    900.00, 35.00, 1215.00, 21,
    CURRENT_DATE - INTERVAL 60 DAY, CURRENT_DATE - INTERVAL 39 DAY,
    'DEFAULTED', @operator_id, @owner_id, 'HIGH_RISK', 82, NOW()
FROM (SELECT 1 AS d WHERE @owner_id IS NOT NULL AND @operator_id IS NOT NULL) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM loans
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084')
      AND status = 'DEFAULTED'
);

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    1, CURRENT_DATE - INTERVAL 7 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE')
      AND installment_number = 1
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    2, CURRENT_DATE - INTERVAL 1 DAY, 812.50, 'OVERDUE'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE')
      AND installment_number = 2
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    3, CURRENT_DATE + INTERVAL 6 DAY, 812.50, 'PENDING'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE')
      AND installment_number = 3
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    4, CURRENT_DATE + INTERVAL 13 DAY, 812.50, 'PENDING'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE')
      AND installment_number = 4
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    1, CURRENT_DATE - INTERVAL 35 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED')
      AND installment_number = 1
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    2, CURRENT_DATE - INTERVAL 28 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED')
      AND installment_number = 2
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    3, CURRENT_DATE - INTERVAL 21 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED')
      AND installment_number = 3
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    4, CURRENT_DATE - INTERVAL 14 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED')
      AND installment_number = 4
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED'),
    1, CURRENT_DATE - INTERVAL 52 DAY, 0.00, 'PAID'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED')
      AND installment_number = 1
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED'),
    2, CURRENT_DATE - INTERVAL 45 DAY, 607.50, 'OVERDUE'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED')
      AND installment_number = 2
) AND @owner_id IS NOT NULL;

INSERT INTO repayment_schedules (loan_id, installment_number, due_date, amount_due, status)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED'),
    3, CURRENT_DATE - INTERVAL 39 DAY, 607.50, 'OVERDUE'
WHERE NOT EXISTS (
    SELECT 1 FROM repayment_schedules
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED')
      AND installment_number = 3
) AND @owner_id IS NOT NULL;

INSERT INTO repayments (loan_id, amount_paid, payment_date, payment_method, captured_by, reference_number)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    812.50, NOW() - INTERVAL 8 DAY, 'EFT', @operator_id, 'DEMO-REPAY-1001'
WHERE NOT EXISTS (SELECT 1 FROM repayments WHERE reference_number = 'DEMO-REPAY-1001') AND @operator_id IS NOT NULL;

INSERT INTO repayments (loan_id, amount_paid, payment_date, payment_method, captured_by, reference_number)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    720.00, NOW() - INTERVAL 28 DAY, 'CASH', @operator_id, 'DEMO-REPAY-1002'
WHERE NOT EXISTS (SELECT 1 FROM repayments WHERE reference_number = 'DEMO-REPAY-1002') AND @operator_id IS NOT NULL;

INSERT INTO repayments (loan_id, amount_paid, payment_date, payment_method, captured_by, reference_number)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    720.00, NOW() - INTERVAL 15 DAY, 'MOBILE_TRANSFER', @operator_id, 'DEMO-REPAY-1003'
WHERE NOT EXISTS (SELECT 1 FROM repayments WHERE reference_number = 'DEMO-REPAY-1003') AND @operator_id IS NOT NULL;

INSERT INTO risk_assessments (borrower_id, loan_id, score, band, summary, created_at)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '9501015009081'),
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    22, 'SAFE', 'Stable income, clean identity data, manageable exposure.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risk_assessments
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE')
) AND @owner_id IS NOT NULL;

INSERT INTO risk_assessments (borrower_id, loan_id, score, band, summary, created_at)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '9102025009082'),
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9102025009082') AND status = 'PENDING'),
    48, 'MEDIUM_RISK', 'Frequent applications and moderate income stress.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risk_assessments
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9102025009082') AND status = 'PENDING')
) AND @owner_id IS NOT NULL;

INSERT INTO risk_assessments (borrower_id, loan_id, score, band, summary, created_at)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '8903035009083'),
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    18, 'SAFE', 'Good repayment history and low fraud indicators.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risk_assessments
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED')
) AND @owner_id IS NOT NULL;

INSERT INTO risk_assessments (borrower_id, loan_id, score, band, summary, created_at)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '8704045009084'),
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED'),
    82, 'HIGH_RISK', 'Default history, low income, and repayment avoidance.', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM risk_assessments
    WHERE loan_id = (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED')
) AND @owner_id IS NOT NULL;

INSERT INTO blacklist_entries (borrower_id, reason, blacklisted_by, blacklisted_at)
SELECT
    (SELECT id FROM borrowers WHERE id_number = '8704045009084'),
    'Defaulted loan and payment avoidance',
    @owner_id,
    NOW() - INTERVAL 30 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM blacklist_entries
    WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084')
) AND @owner_id IS NOT NULL;

INSERT INTO notifications (user_id, channel, message, status, created_at)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.one'),
    'PUSH',
    'Your loan is active. Installment 2 is now overdue.',
    'SENT',
    NOW() - INTERVAL 1 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE user_id = (SELECT id FROM users WHERE username = 'demo.borrower.one')
      AND message = 'Your loan is active. Installment 2 is now overdue.'
) AND @owner_id IS NOT NULL;

INSERT INTO notifications (user_id, channel, message, status, created_at)
SELECT
    (SELECT id FROM users WHERE username = 'demo.borrower.two'),
    'SMS',
    'Your loan application is pending owner review.',
    'SENT',
    NOW() - INTERVAL 2 HOUR
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE user_id = (SELECT id FROM users WHERE username = 'demo.borrower.two')
      AND message = 'Your loan application is pending owner review.'
) AND @owner_id IS NOT NULL;

INSERT INTO notifications (user_id, channel, message, status, created_at)
SELECT
    @owner_id,
    'SYSTEM',
    'Demo dataset loaded: pending, active, completed, and defaulted loans are available for review.',
    'SENT',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM notifications
    WHERE user_id = @owner_id
      AND message = 'Demo dataset loaded: pending, active, completed, and defaulted loans are available for review.'
) AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    2500.00, 'DISBURSEMENT', 'DEMO-DISB-1001', NOW() - INTERVAL 14 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-DISB-1001') AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    1200.00, 'DISBURSEMENT', 'DEMO-DISB-1002', NOW() - INTERVAL 42 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-DISB-1002') AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084') AND status = 'DEFAULTED'),
    900.00, 'DISBURSEMENT', 'DEMO-DISB-1003', NOW() - INTERVAL 60 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-DISB-1003') AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    812.50, 'REPAYMENT', 'DEMO-REPAY-1001', NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-REPAY-1001') AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    720.00, 'REPAYMENT', 'DEMO-REPAY-1002', NOW() - INTERVAL 28 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-REPAY-1002') AND @owner_id IS NOT NULL;

INSERT INTO cash_transactions (loan_id, amount, type, reference_number, captured_at)
SELECT
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8903035009083') AND status = 'COMPLETED'),
    720.00, 'REPAYMENT', 'DEMO-REPAY-1003', NOW() - INTERVAL 15 DAY
WHERE NOT EXISTS (SELECT 1 FROM cash_transactions WHERE reference_number = 'DEMO-REPAY-1003') AND @owner_id IS NOT NULL;

INSERT INTO audit_logs (user_id, action, entity, entity_id, details, timestamp)
SELECT @operator_id, 'CREATE_BORROWER', 'Borrower',
    (SELECT id FROM borrowers WHERE id_number = '9501015009081'),
    'Loaded demo borrower Naledi Dlamini', NOW() - INTERVAL 14 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'CREATE_BORROWER' AND details = 'Loaded demo borrower Naledi Dlamini'
) AND @operator_id IS NOT NULL;

INSERT INTO audit_logs (user_id, action, entity, entity_id, details, timestamp)
SELECT @operator_id, 'APPLY_LOAN', 'Loan',
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9102025009082') AND status = 'PENDING'),
    'Demo pending application captured for Thabo Mokoena', NOW() - INTERVAL 2 HOUR
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'APPLY_LOAN' AND details = 'Demo pending application captured for Thabo Mokoena'
) AND @operator_id IS NOT NULL;

INSERT INTO audit_logs (user_id, action, entity, entity_id, details, timestamp)
SELECT @owner_id, 'APPROVE_LOAN', 'Loan',
    (SELECT id FROM loans WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '9501015009081') AND status = 'ACTIVE'),
    'Demo active loan approved for Naledi Dlamini', NOW() - INTERVAL 14 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'APPROVE_LOAN' AND details = 'Demo active loan approved for Naledi Dlamini'
) AND @owner_id IS NOT NULL;

INSERT INTO audit_logs (user_id, action, entity, entity_id, details, timestamp)
SELECT @operator_id, 'RECORD_REPAYMENT', 'Repayment',
    (SELECT id FROM repayments WHERE reference_number = 'DEMO-REPAY-1001'),
    'Demo repayment received for active loan', NOW() - INTERVAL 8 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'RECORD_REPAYMENT' AND details = 'Demo repayment received for active loan'
) AND @operator_id IS NOT NULL;

INSERT INTO audit_logs (user_id, action, entity, entity_id, details, timestamp)
SELECT @owner_id, 'BLACKLIST_BORROWER', 'BlacklistEntry',
    (SELECT id FROM blacklist_entries WHERE borrower_id = (SELECT id FROM borrowers WHERE id_number = '8704045009084')),
    'Demo blacklist entry created for defaulted borrower', NOW() - INTERVAL 30 DAY
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs
    WHERE action = 'BLACKLIST_BORROWER' AND details = 'Demo blacklist entry created for defaulted borrower'
) AND @owner_id IS NOT NULL;
