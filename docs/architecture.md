# Architecture Overview

## Runtime Components

- `apps/mobile`: Kotlin Android borrower app using Retrofit and Room
- `apps/web`: Vue 2 Owner/Cashier portal using Vue Router, Vuex, and Axios
- `services/api`: Spring Boot API with JWT authentication, JPA, Flyway, risk logic, and audit logging
- `database`: SQL schema and seed scripts for local MySQL setups

## Core Data Flow

1. A user authenticates through the Spring Boot API and receives a JWT.
2. Cashier or borrower submits a loan application.
3. The API calculates a risk score and stores a `risk_assessments` row.
4. The owner approves or rejects the loan.
5. Approved loans generate repayment schedules and a cash disbursement record.
6. Repayments update schedules, loan status, cash transactions, and audit logs.

## Security Controls

- BCrypt password hashing
- JWT token-based API access
- Role-based endpoint authorization for `OWNER`, `CASHIER`, and `BORROWER`
- Basic rate limiting on auth and loan application routes
- Immutable audit trail for critical state changes

## Seed Credentials

- `owner` / `Password1!`
- `cashier` / `Password1!`
- `borrower.demo` / `Password1!`
