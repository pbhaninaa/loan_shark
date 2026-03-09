# LendHand — Release Notes

**Release:** Production (v1.0)  
**Date:** March 2025  
**Product:** LendHand — Loan Management System for Micro-Lenders

---

## Overview

LendHand is a full-stack loan management system for micro-lenders and loan providers. This release delivers a web portal for staff (Owner and Cashier) and borrowers, with configurable interest and terms, repayment tracking, KYC-style verification, and role-based access control.

---

## What’s in This Release

### Authentication & Security

- **JWT-based authentication** — Stateless login with configurable token expiry and secret (via `application.properties`).
- **Role-based access** — Three roles: **Owner**, **Cashier**, **Borrower**. Menus and API endpoints are restricted by role.
- **Password reset** — Forgot-password flow with email link; owner can reset another user’s password.
- **Rate limiting** — Configurable limits on auth and sensitive endpoints to reduce abuse.
- **Secure credentials** — Passwords hashed with BCrypt; no reversible storage.

### Owner & Cashier (Operations Portal)

- **Dashboard** — Summary metrics (e.g. total loans, active borrowers, overdue count, repayments captured). Cashiers have access to the summary view.
- **Audit logs & actions** — Owner can view audit logs and recent actions for accountability.
- **Borrowers** — List, search, create (with or without documents), edit, and manage status. View borrower documents (ID, selfie).
- **Loans** — List all loans with amount and status. **Approve** or **reject** applications; owners can approve any loan; cashiers can approve/reject loans **under a configurable amount** (default R10,000).
- **Repayments** — **Capture payment** with:
  - Dropdown of **active loans only**.
  - **Payer (borrower) full name** shown when selecting a loan and in repayment history to avoid crediting the wrong account.
  - **Auto-generated reference numbers** (e.g. PAY-1001, PAY-1002) based on the last repayment in the system.
  - Payment method (e.g. CASH, EFT, MOBILE_TRANSFER) and reference.
- **Repayment history** — View repayments per loan with payer name, amount, method, reference, and who recorded it.
- **Users** — Owner can create staff (e.g. Cashier), manage accounts, and reset passwords.
- **Verifications** — Owner reviews borrower verification requests (e.g. ID and selfie), approve or reject with notes.
- **Blacklist** — Owner can blacklist borrowers; blacklisted borrowers are blocked from new loans.
- **Loan interest & term settings** — Owner configures default interest rate, interest type, period, grace period, default term, and **borrower limit** (max loan as a percentage of client’s monthly income).
- **Business capital** — Owner views lending pool balance and can top up; repayments feed back into the pool.

### Borrower (Client Portal)

- **Registration** — Self-register with username, password, personal details, and optional ID/selfie upload.
- **Profile** — View and update own profile (e.g. contact, employment, income).
- **My loans** — Apply for loans; view application status (e.g. PENDING, ACTIVE, COMPLETED, REJECTED).
- **Borrower limit** — Loan amount is limited to a percentage of monthly income (set by owner in Loan Interest & Term settings).
- **Repayment schedule** — View installments (due date, amount due, status) for active loans.
- **Repayments** — Borrowers can record repayments against their own loans only.
- **Notifications** — In-app notifications for loan status, repayments, and profile/verification updates.

### Loan Lifecycle & Repayments

- **Application** — Borrower or staff submits amount (and optional term). System applies interest and term from settings and enforces borrower limit.
- **Approval** — Owner or (for smaller loans) Cashier approves or rejects; on approval, repayment schedule is generated and loan becomes ACTIVE.
- **Repayments** — Each payment is applied to the selected loan’s schedule (installments); that **borrower’s debt is reduced**. When all installments are paid, loan moves to COMPLETED.
- **Cash flow** — Repayments are reflected in business capital and in dashboard “repayments captured” metrics.

### Configuration & Operations

- **Centralised configuration** — Key behaviour is driven by `application.properties` (and profile-specific files), including:
  - JWT secret and expiry
  - Database and JPA
  - Frontend URL (CORS)
  - Loan rules (e.g. cashier approval limit, default installments)
  - Rate limiting
  - Mail (for password reset)
  - Lender/brand name (e.g. LendHand Lending)
- **First-time setup** — Create the initial owner account via the web UI; no seed owner in DB.

### User Experience & Branding

- **LendHand branding** — Product name, logo (helping-hand style), and favicon across login and sidebar.
- **Responsive layout** — Scrollable content when the viewport is small; layout adapts to different screen sizes.
- **Clear payer context** — When capturing a payment, the system shows who the payment is for (borrower full name) so staff can confirm they are crediting the correct account.

### Technical Highlights

- **Backend:** Java 17+, Spring Boot, Spring Security, JPA, Flyway, MySQL.
- **Frontend:** Vue 3, Vuetify 3, Pinia, Vue Router.
- **Auth:** JWT in headers; optional email required only on first login (client-side redirect to account page).
- **APIs:** REST; JSON; error responses and 401/403 handling for auth and access control.

---

## Known Limitations & Notes

- **Bank statement verification** — A `BankStatementChecker` utility exists for future use (e.g. consistency and income checks). It is **not** wired into any flow in this release.
- **Package and internal names** — Java packages and some internal identifiers still use the legacy name; only user-facing branding is “LendHand”.
- **LocalStorage keys** — Session keys (e.g. `loanSharkToken`) are unchanged for backward compatibility with existing sessions.

---

## Upgrade / Deployment

- Run Flyway migrations against your database so all tables and columns (e.g. `borrower_limit_percentage`, repayment and loan response fields) are up to date.
- Set `jwt.secret`, database URL, and (if used) mail and frontend URL in your environment or `application-<profile>.properties`.
- Build and deploy the API and web app per your existing process (see `DEPLOYMENT.md` and `README.md`).

---

## Support & Documentation

- **User manual:** `USER_MANUAL.md` and `docs/USER_MANUAL.md`
- **Deployment:** `DEPLOYMENT.md`
- **Repository:** See project README and repo URL in your environment.

Thank you for using LendHand.
