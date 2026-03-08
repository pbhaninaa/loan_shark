# Backend (services/api) – Environment variables

Used when running locally or when deploying to Railway. See **DEPLOYMENT.md** at repo root for full Railway + Vercel setup.

## Required for deployment (Railway)

| Variable | Description |
|----------|-------------|
| `MYSQL_PUBLIC_URL` | MySQL connection URL (e.g. from Railway MySQL → Variables → MYSQL_PUBLIC_URL). Use reference `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}` when backend is on Railway. |
| `JWT_SECRET` | Secret for JWT signing (min 64 characters). |

## Optional

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `sit`, `uat`, or `prod`. |
| `APP_FRONTEND_URL` | Frontend base URL (e.g. Vercel URL) for password reset links. |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP / mail sender. |
| `STRIPE_SECRET_KEY` | Stripe API secret key. |
| `JWT_EXPIRATION_MS` | Token expiry in ms (default 3600000). |

## Local (application-local.properties)

Copy `application-local.example.properties` to `application-local.properties` and set either:

- **Option A:** `MYSQL_URL` = Railway public URL (same DB as deployed), or  
- **Option B:** `spring.datasource.url`, `username`, `password` for a local MySQL.

Also set `jwt.secret` (or `JWT_SECRET` env), mail, and other overrides as needed.
