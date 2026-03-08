# Environments: SIT, UAT, PROD

| Environment | Who uses it | Frontend (apps/web) | Backend (services/api) | Connection |
|-------------|-------------|---------------------|------------------------|------------|
| **SIT** | Devs (local) | `npm run dev` → localhost:5173 | Run locally on port 8080, profile `sit` | Frontend calls `http://localhost:8080` |
| **UAT** | Testers | Deploy to Vercel, set `VITE_API_URL` to Railway UAT backend | Deploy to Railway, set `SPRING_PROFILES_ACTIVE=uat`, `APP_FRONTEND_URL` = Vercel UAT URL | Deployed frontend ↔ deployed UAT backend |
| **PROD** | Live | Deploy to Vercel, set `VITE_API_URL` to Railway PROD backend | Deploy to Railway, set `SPRING_PROFILES_ACTIVE=prod`, `APP_FRONTEND_URL` = Vercel PROD URL | Deployed frontend ↔ deployed PROD backend |

## Frontend (apps/web – Vite)

- **SIT:** Leave `VITE_API_URL` unset or set in `.env` → defaults to `http://localhost:8080`. Run with `npm run dev`.
- **UAT:** Vercel env vars → `VITE_API_URL` = your Railway UAT backend URL (e.g. `https://backend-uat.xxx.up.railway.app`). Build: `npm run build`.
- **PROD:** Vercel env vars → `VITE_API_URL` = your Railway PROD backend URL. Build: `npm run build`.

Override `VITE_API_URL` in Vercel per project/environment so deployed UAT and PROD use the correct backend URLs.

## Backend (services/api – Spring Boot)

- **SIT:** Default profile when running locally. `application-local.properties` or env can set `app.frontend-url=http://localhost:5173`. Use `MYSQL_URL` or local DB.
- **UAT:** On Railway set:
  - `SPRING_PROFILES_ACTIVE=uat`
  - `MYSQL_PUBLIC_URL` (or DB vars) from Railway MySQL UAT
  - `APP_FRONTEND_URL` = your Vercel UAT frontend URL (for password reset emails)
- **PROD:** On Railway set:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `MYSQL_PUBLIC_URL` (or DB vars) from Railway MySQL PROD
  - `APP_FRONTEND_URL` = your Vercel PROD frontend URL

Database and other secrets: set per service in Railway (e.g. UAT vs PROD DB if you split them).
