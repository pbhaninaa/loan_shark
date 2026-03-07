DELETE FROM notifications
WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('owner', 'cashier', 'borrower.demo')
);

DELETE FROM borrower_documents
WHERE borrower_id IN (
    SELECT id FROM borrowers WHERE user_id IN (
        SELECT id FROM users WHERE username = 'borrower.demo'
    )
);

DELETE FROM borrowers
WHERE user_id IN (
    SELECT id FROM users WHERE username = 'borrower.demo'
);

DELETE FROM users
WHERE username IN ('owner', 'cashier', 'borrower.demo');
