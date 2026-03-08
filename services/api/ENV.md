# Backend (services/api) – Environment variables

DB is configured **only via properties / env** (same as Mechanic Management). No custom DB code.

See **DEPLOYMENT.md** at repo root for full Railway + Vercel setup.

## Required for deployment (Railway)

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL, e.g. `jdbc:mysql://host:port/database?useSSL=true`. On Railway: use the **public** MySQL URL from your MySQL service (convert `mysql://...` to `jdbc:mysql://...` or set from Railway Variables). |
| `SPRING_DATASOURCE_USERNAME` | MySQL username (e.g. from Railway MySQL service). |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password (e.g. from Railway MySQL service). |
| `JWT_SECRET` | Secret for JWT signing (min 64 characters). |

## Optional

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Exactly one of: `sit`, `uat`, or `prod`. |
| `APP_FRONTEND_URL` | Frontend base URL (e.g. Vercel URL) for password reset links. |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP / mail sender. |
| `STRIPE_SECRET_KEY` | Stripe API secret key. |
| `JWT_EXPIRATION_MS` | Token expiry in ms (default 3600000). |

## Local (application-local.properties)

Copy `application-local.example.properties` to `application-local.properties` and set:

- `spring.datasource.password` (for sit profile; url/username are in application-sit.properties), or
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` to override for local MySQL.

Also set `jwt.secret` (or `JWT_SECRET` env), mail, and other overrides as needed.
