-- Add email to users for password reset (owner/cashier/staff). Borrowers use borrowers.email.
ALTER TABLE users ADD COLUMN email VARCHAR(160) NULL;
