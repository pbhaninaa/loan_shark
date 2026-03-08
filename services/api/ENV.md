# Backend (services/api) – Environment variables

Used when running locally or when deploying to Railway. See **DEPLOYMENT.md** at repo root for full Railway + Vercel setup.

## Required for deployment (Railway)

When the Backend is linked to a Railway MySQL service, Railway injects `MYSQL_URL`, `MYSQL_PUBLIC_URL`, `MYSQLHOST`, `MYSQLUSER`, etc. The app **prefers `MYSQL_PUBLIC_URL`** and sets `spring.datasource.*` from it (same behaviour as the working backend that uses `SPRING_DATASOURCE_*`). No need to add `SPRING_DATASOURCE_URL` manually.

| Variable | Description |
|----------|-------------|
| `MYSQL_PUBLIC_URL` | From MySQL service. Ensure the Backend has it (e.g. reference `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}` or link the MySQL service so Railway injects it). The app uses this over `MYSQL_URL` so the private host is never used. |
| `JWT_SECRET` | Secret for JWT signing (min 64 characters). |

## Optional

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Exactly one of: `sit`, `uat`, or `prod` (e.g. `prod` for production). Do not use `prod or sit`. |
| `APP_FRONTEND_URL` | Frontend base URL (e.g. Vercel URL) for password reset links. |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP / mail sender. |
| `STRIPE_SECRET_KEY` | Stripe API secret key. |
| `JWT_EXPIRATION_MS` | Token expiry in ms (default 3600000). |

## Local (application-local.properties)

Copy `application-local.example.properties` to `application-local.properties` and set either:

- **Option A:** `MYSQL_URL` = Railway public URL (same DB as deployed), or  
- **Option B:** `spring.datasource.url`, `username`, `password` for a local MySQL.

Also set `jwt.secret` (or `JWT_SECRET` env), mail, and other overrides as needed.
