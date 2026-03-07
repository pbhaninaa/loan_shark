# Loan Shark Loan Management System

A greenfield monorepo for a micro-lending platform with:

- `services/api`: Spring Boot API with JWT auth, risk checks, repayments, reporting, and audit logs
- `apps/web`: Vue 2 Owner/Cashier portal
- `apps/mobile`: Android borrower app in Kotlin
- `database`: SQL schema and seed data
- `docs`: architecture and API notes

## Modules

- Backend system of record for loans, borrowers, repayments, blacklist, and risk
- Web portal for owner and cashier workflows
- Android app for borrower self-service

## MVP Lifecycle

1. Cashier registers a borrower
2. Cashier or borrower submits a loan application
3. System calculates risk and records an assessment
4. Owner approves or rejects the loan
5. System generates a repayment schedule
6. Cashier records repayments
7. Dashboard and audit logs reflect all critical actions
