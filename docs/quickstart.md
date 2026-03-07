# Quickstart

## Backend

1. Create a MySQL database named `loan_shark`.
2. Update `services/api/src/main/resources/application.yml` if your local MySQL credentials differ.
3. Run the Spring Boot service from `services/api`.

Seeded logins:

- `owner` / `Password1!`
- `cashier` / `Password1!`
- `borrower.demo` / `Password1!`

## Web

```bash
cd apps/web
npm install
npm run build
npm run dev
```

## Android

1. Open `apps/mobile` in Android Studio.
2. Let Gradle sync the project.
3. Start an emulator and run the app.
4. Ensure the backend is reachable at `http://10.0.2.2:8080`.

## First Demo Flow

1. Log into the web portal as `cashier`.
2. Create a borrower or use seeded borrower `1`.
3. Capture a loan application.
4. Log in as `owner` and approve the loan.
5. Capture a repayment from the repayments screen.
6. Check owner dashboard summaries and audit logs.
