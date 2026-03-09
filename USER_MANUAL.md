# LendHand — User Manual

**Loan Management System for Micro-Lenders**

This manual describes how to use the web portal and the main workflows for Owners, Staff (Cashiers), and Borrowers.

---

## Table of contents

1. [System overview](#1-system-overview)
2. [User roles](#2-user-roles)
3. [Getting started](#3-getting-started)
4. [Owner](#4-owner)
5. [Staff (Cashier)](#5-staff-cashier)
6. [Borrower](#6-borrower)
7. [Business rules (interest & terms)](#7-business-rules-interest--terms)
8. [Notifications](#8-notifications)
9. [Running the system](#9-running-the-system)

---

## 1. System overview

The system has three parts:

- **Backend API** — Spring Boot service (e.g. runs on port 8080).
- **Web portal** — Vue 3 + Vuetify (e.g. runs on port 5174). Used by staff and borrowers.
- **Android app** (optional) — For borrowers on mobile.

**Main features:**

- User accounts: Owner, Staff (Cashier), Borrower.
- Borrower self-registration with **KYC** (ID check, location, ID copy PDF, live selfie).
- Manual verification of new borrowers by the Owner.
- Loan applications (amount only; interest and term come from admin settings).
- Repayment capture and history (with payer shown).
- Notifications (in-app and email).
- Audit trail (who did what and when).
- Blacklist and configurable interest/grace period/default term.

---

## 2. User roles

| Role      | Who uses it        | Main access |
|----------|--------------------|-------------|
| **Owner**   | Business owner/admin | Full access: dashboard, borrowers, loans, repayments, **users**, **verifications**, **blacklist**, **loan interest & term settings**. |
| **Cashier** | Staff               | Dashboard, borrowers, loans, repayments. Cannot manage users, verifications, blacklist, or loan settings. |
| **Borrower**| Customer            | Own profile, loans, repayment schedule, notifications. Must be **verified (ACTIVE)** before using loans. |

---

## 3. Getting started

### 3.1 Logging in

1. Open the web portal (e.g. `http://localhost:5174`).
2. Go to the **Login** section.
3. Enter **Username** and **Password**.
4. Click **Login**. You are taken to:
   - **Owner/Cashier:** Dashboard.
   - **Borrower (ACTIVE):** My Profile (borrower portal).
   - **Borrower (not yet verified):** Verification Status page (no other actions until verified).

### 3.2 First-time setup (Owner)

1. **Register as Owner** (one-time):
   - On the login page, use the **Create owner account** section.
   - Enter username, password, and any required details.
   - Submit. You can then log in as Owner.

2. **Create staff (Cashier) accounts:**
   - Log in as Owner.
   - Open **Users** from the menu.
   - Click **Create** (or “Create Staff Account”).
   - Fill in username, password, role **Cashier**, and any other fields.
   - Save. Staff can then log in with those credentials.

3. **Configure loan business rules:**
   - Go to **Loan interest & term** in the menu.
   - Set default interest rate, type (Simple/Compound), interest period (days), grace period (days), and default loan term (days).
   - Save. These apply when borrowers or staff submit a loan with **only the amount** (no rate or term entered).

### 3.3 Borrower registration (self-service)

1. On the login page, open the **Create your borrower account** section.
2. Complete the **multi-step KYC** flow:
   - **Step 1 — Personal details:** Username, password, name, South African ID number, phone, email, address, employment, income, employer. The system checks that the ID number is valid (SA format).
   - **Step 2 — Location:** Location is captured **automatically** when the step loads. You must allow browser/device location. If you deny it, you cannot complete registration.
   - **Step 3 — ID copy:** Upload a **clear PDF** of your ID document.
   - **Step 4 — Selfie:** Take a **live selfie** with the camera (no file upload). The system compares it to the ID photo.
   - **Step 5 — Review & submit:** Check the summary and submit.
3. After submit:
   - Your profile is created and you can **log in**.
   - Your status is **Pending verification**. You will see the **Verification Status** page only; you cannot apply for loans or use other borrower features until the Owner approves you.
4. When the Owner **approves** your verification, your status becomes **ACTIVE**. You then get access to **My Profile**, **My Loans**, **Repayment Schedule**, and **Notifications**.

**If registration fails:** The message on screen will describe the problem (e.g. invalid ID, location declined, selfie/ID mismatch). Fix the issue and try again.

---

## 4. Owner

### 4.1 Dashboard

- Summary of key numbers and **Recent Actions** (last 5 rows).  
- Actions show who did what and when; long text is shortened with a tooltip for full details.

### 4.2 Borrowers

- List of all borrowers (search and pagination at the top).
- **Create borrower (admin):** Owner can create a borrower **without** image verification (no ID PDF/selfie). All other checks (e.g. SA ID format) still apply. Use this when documents cause issues.
- View/edit borrower details and **change borrower status** (e.g. ACTIVE, PENDING_VERIFICATION, BLACKLISTED) in the borrower table.

### 4.3 Loans

- View and manage loan applications (search and pagination).
- Create a loan on behalf of a borrower: select borrower and enter **amount** only; interest and term come from **Loan interest & term** settings.

### 4.4 Repayments

- Record repayments (amount, method, reference, etc.).
- Repayment history shows **Payer** (who captured the payment) and is searchable with pagination.

### 4.5 Users (Owner only)

- **CRUD all users** (Owner, Cashier, Borrower).
- Create staff (Cashier) accounts.
- Change roles and status as needed.

### 4.6 Verifications (Owner only)

- **Manual review queue:** List of borrower verifications that need approval or rejection (e.g. when auto-check could not confirm ID/selfie).
- For each case you can:
  - View **ID copy (PDF)** and **selfie** to confirm they match the person.
  - **Approve** or **Reject** with optional notes.
- Approved borrowers become ACTIVE and can use the system; rejected ones stay limited until corrected or re-submitted.

### 4.7 Blacklist (Owner only)

- Add/remove blacklist entries (e.g. by borrower or reason).
- Table supports search and pagination; long reasons are truncated with a tooltip for full text.

### 4.8 Loan interest & term (Owner only)

- **Default interest rate** — Used when a loan is created with only amount.
- **Interest type** — Simple or Compound.
- **Interest period (days)** — e.g. 30 days; interest accrues per this period.
- **Grace period (days)** — Days after a due date during which interest does **not** accrue (e.g. 2 days).
- **Default loan term (days)** — Used when the borrower (or staff) does not specify a term (e.g. 365).
- Saving here updates the single global settings row; new and existing calculations use these values as configured.

---

## 5. Staff (Cashier)

- **Dashboard** — Same as Owner but without owner-only metrics; recent actions limited to 5 rows.
- **Borrowers** — View and search; create borrower only if given that permission (otherwise Owner does it).
- **Loans** — View and create loans (amount only; rate/term from settings).
- **Repayments** — Capture repayments and view history (including payer).
- No access to: **Users**, **Verifications**, **Blacklist**, **Loan interest & term**.

---

## 6. Borrower

### 6.1 Before verification (PENDING_VERIFICATION)

- After registration you can **log in** but only see **Verification Status**.
- This page explains that your profile is under review.
- You cannot open **My Profile**, **My Loans**, **Repayment Schedule**, or **Notifications** until status is **ACTIVE**.

### 6.2 After verification (ACTIVE)

- **My Profile** — View your borrower details.
- **My Loans** — See your loans and **apply for a new loan** (enter **amount only**; interest and term are set by the business).
- **Repayment Schedule** — See when and how much to pay.
- **Notifications** — In-app list; notifications are marked read and can disappear from the list (but stay in the database). You also get **email** notifications for important events (profile created, status change, application status change).

---

## 7. Business rules (interest & terms)

- **Interest** starts when the borrower **receives** the money (e.g. from first day).
- It then accrues every **interest period** (e.g. every 30 days), depending on **Simple** or **Compound** setting in **Loan interest & term**.
- **Grace period:** For a given due date, no extra interest is added for that many days (e.g. 2 days). After the grace period, interest accrues as per settings.
- **Term:** The borrower does **not** choose a fixed repayment period. They only choose the **amount**. The **default loan term (days)** from admin is used for due date and interest calculation. Repayment is flexible: any payment reduces what they owe, and interest continues per business rules until the loan is fully paid.

---

## 8. Notifications

- **In-app:** Shown under **Notifications** in the borrower portal. After reading, they can be removed from the list but remain stored.
- **Email:** Sent for:
  - Profile created
  - Borrower status change
  - Loan application status change  
  Email requires correct SMTP configuration in the backend (see application configuration).

---

## 9. Running the system

### Backend (API)

- **With Maven in PATH:**  
  `cd services\api` then `mvn spring-boot:run`
- **Without Maven:** Install Maven (e.g. `winget install Apache.Maven`), close and reopen the terminal, then run the same command. Or run the main class `com.loanshark.api.LoanSharkApiApplication` from your IDE.
- Ensure **MySQL** is running and the `loan_shark` database exists. The API runs **Flyway** on startup to apply migrations (e.g. creates `loan_interest_settings` and other tables).

### Web portal

- `cd apps\web` then `npm install` (once) and `npm run dev`.
- Open the URL shown (e.g. `http://localhost:5174`) and use **Login** or **Create your borrower account** as needed.

### First-time database

- Create the database `loan_shark` in MySQL (e.g. via MySQL Workbench).
- Start the API once; Flyway will create and update tables. The **Loan interest & term** page will work once the `loan_interest_settings` row exists (created by migration or by the app on first load).

---

*For technical details, see the project README and the source code in `services/api`, `apps/web`, and `apps/mobile`.*
