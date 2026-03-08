# Deploy: Railway (DB + Backend) + Vercel (Frontend)

This guide sets up **MySQL and Backend on Railway**, and **Frontend on Vercel**, so they work together.

---

## 1. Railway – MySQL database

1. In your Railway project, click **+ New** → **Database** → **MySQL**.
2. Wait for the MySQL service to start. Note its name (e.g. `MySQL-Q-C2`).

No extra config needed; Railway provides the connection variables.

---

## 2. Railway – Backend API

1. **+ New** → **GitHub Repo** (or use an existing service), and select `pbhaninaa/loan_shark`.
2. Set **Root Directory** (or **Source**) to **`services/api`** so the Dockerfile in that folder is used.
3. Under **Variables**, add at least:

| Variable | Value |
|----------|--------|
| `MYSQL_URL` | `${{ YourMySQLServiceName.MYSQL_URL }}` |
| `SPRING_PROFILES_ACTIVE` | `sit` or `prod` |
| `JWT_SECRET` | A long random string (min 64 characters for HS512) |
| `APP_FRONTEND_URL` | Your Vercel app URL, e.g. `https://your-app.vercel.app` |
| `APP_FRONTEND_ALLOWED_ORIGINS` | Same URL + previews, e.g. `https://your-app.vercel.app,https://*.vercel.app` |

Replace `YourMySQLServiceName` with your MySQL service name (e.g. `MySQL-Q-C2`).

**Optional (for mail, Stripe, etc.):**

- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `STRIPE_SECRET_KEY`

4. Deploy. After deploy, open **Settings** → **Networking** → **Generate domain** so the Backend has a public URL (e.g. `https://your-backend.up.railway.app`). Copy that URL for the frontend.

---

## 3. Vercel – Frontend

1. Import the repo (e.g. `pbhaninaa/loan_shark`) into Vercel.
2. Set **Root Directory** to **`apps/web`**.
3. Under **Environment Variables**, add:

| Name | Value |
|------|--------|
| `VITE_API_URL` | Your Railway Backend URL, e.g. `https://your-backend.up.railway.app` |

No trailing slash. Use the same URL you copied from Railway Backend (Settings → Networking → domain).

4. Deploy. The frontend will call the Backend on Railway, and the Backend will allow CORS for your Vercel origin(s) because of `APP_FRONTEND_ALLOWED_ORIGINS`.

---

## Checklist

- [ ] Railway: MySQL service created.
- [ ] Railway: Backend service from repo, root directory `services/api`, variables set (`MYSQL_URL`, `JWT_SECRET`, `APP_FRONTEND_URL`, `APP_FRONTEND_ALLOWED_ORIGINS`, …).
- [ ] Railway: Backend has a public domain; URL copied.
- [ ] Vercel: Frontend from repo, root directory `apps/web`, `VITE_API_URL` = Backend URL.
- [ ] Backend and frontend redeployed after any env change.

---

## Troubleshooting

- **CORS errors in browser:** Ensure `APP_FRONTEND_ALLOWED_ORIGINS` on Railway Backend includes the exact Vercel URL (and `https://*.vercel.app` if you use preview deployments).
- **Backend can’t connect to MySQL:** Ensure `MYSQL_URL` on the Backend is the **reference** `${{ YourMySQLServiceName.MYSQL_URL }}` so it uses the private Railway URL.
- **Frontend gets 404 / wrong API:** Ensure `VITE_API_URL` on Vercel is the Backend public URL (no trailing slash) and redeploy the frontend after changing it.
