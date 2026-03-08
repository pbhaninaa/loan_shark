# What to set where – Railway (Backend + DB) and Vercel (Frontend)

Same deployment pattern as **Mechanic-Management**: Railway for backend + MySQL, Vercel for frontend.

---

## Backend won’t start: “railway.internal not reachable” or “add MYSQL_PUBLIC_URL”

If the backend fails with **“MYSQL_URL points to Railway's private host (railway.internal), which is not reachable”**:

1. Open your **Railway project** → select the **Backend** service (the Java/Spring one).
2. Go to **Variables**.
3. **Add** a variable (do not replace `MYSQL_URL`):
   - **Name:** `MYSQL_PUBLIC_URL`
   - **Value:** `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}`  
     Replace `YourMySQLServiceName` with your MySQL service name (e.g. `MySQL-Q-C2` or `mysql`). Use the exact name shown in the Variables dropdown when you type `${{`.
4. Fix **SPRING_PROFILES_ACTIVE** if needed: set it to **exactly** `prod` or `uat` or `sit` (one value only, not e.g. `prod or sit`).
5. **Redeploy** the Backend (Deployments → ⋮ → Redeploy, or push a commit).

The app uses `MYSQL_PUBLIC_URL` when the private URL is not reachable, so the backend can connect to MySQL.

---

## 1. Railway (Backend + Database)

**Repo layout:** Backend lives in **`services/api`**. In Railway, set **Root Directory** to **`services/api`** (Dockerfile there).

In your **Railway project** (backend service), open **Variables** and set:

### Required

| Variable | Where to get it | Example |
|----------|-----------------|---------|
| `MYSQL_PUBLIC_URL` | Railway MySQL → **Variables** → copy value of **MYSQL_PUBLIC_URL**, or reference: `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}` | `mysql://root:xxx@gondola.proxy.rlwy.net:49401/railway` |
| `JWT_SECRET` | Generate a long random string (min 64 chars). E.g. `openssl rand -base64 48` | `your_64_char_secret_...` |

### Optional (DB – if not using MYSQL_PUBLIC_URL)

| Variable | Where to get it |
|----------|-----------------|
| `SPRING_DATASOURCE_URL` | JDBC URL from Railway MySQL (Connect / Variables). Use **public** URL when private host does not resolve. |
| `SPRING_DATASOURCE_USERNAME` | Railway MySQL → Variables (often `root`) |
| `SPRING_DATASOURCE_PASSWORD` | Railway MySQL → Variables |

### Frontend URL (for emails and links)

| Variable | Value | Notes |
|----------|--------|------|
| `APP_FRONTEND_URL` | Your **Vercel production URL** (no trailing slash) | e.g. `https://loan-shark-xxx.vercel.app` |
| `APP_FRONTEND_ALLOWED_ORIGINS` | Optional. CORS allows all origins by default; set this if you restrict later. | Comma-separated origins |

### Profile

| Variable | Value |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `sit` or `prod` |

### Optional (defaults in code)

- `MAIL_HOST` – default `smtp.gmail.com`
- `MAIL_PORT` – default `587`
- `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` – for mail
- `STRIPE_SECRET_KEY` – for payments
- `JWT_EXPIRATION_MS` – default `3600000` (1 hour)

---

## 2. Vercel (Frontend – apps/web)

**Repo layout:** Frontend lives in **`apps/web`**. In Vercel, set **Root Directory** to **`apps/web`** (or use root and rely on `vercel.json`).

In your **Vercel project** → **Settings** → **Environment Variables**, add:

### Required

| Variable | Value | Environment |
|----------|--------|-------------|
| `VITE_API_URL` | Your **Railway backend URL** (no trailing slash, no `/api`) | Production (and Preview if you use it) |
| `VITE_APP_ENV` | `production` | Production |

**Example:** If your Railway backend is `https://your-backend.up.railway.app`, set:
- `VITE_API_URL` = `https://your-backend.up.railway.app`

### Optional (for Stripe card UI)

| Variable | Value |
|----------|--------|
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe **Publishable key** (e.g. `pk_live_...`) from [Stripe API Keys](https://dashboard.stripe.com/apikeys) |

---

## 3. Quick checklist

**Railway (backend service)**
- [ ] Root Directory = **`services/api`**
- [ ] `SPRING_PROFILES_ACTIVE` = `sit` or `prod`
- [ ] `MYSQL_PUBLIC_URL` = `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}` (or paste value)
- [ ] `JWT_SECRET` (min 64 chars)
- [ ] `APP_FRONTEND_URL` = your Vercel app URL (optional but recommended)
- [ ] Generate **public domain** in Settings → Networking

**Vercel (apps/web)**
- [ ] Root Directory = **`apps/web`**
- [ ] `VITE_API_URL` = `https://YOUR-RAILWAY-APP.up.railway.app` (no trailing slash)
- [ ] `VITE_APP_ENV` = `production`

After changing env vars on **Vercel**, trigger a new deployment so the build picks them up.

---

## 4. Exact variable names (prod deployment reference)

Copy each **Variable** name into the platform’s “Name” field and the described value in the “Value” field.

### Railway (Backend service) → Variables

| Variable (Name) | Value / What to put in the field |
|-----------------|----------------------------------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `MYSQL_PUBLIC_URL` | `${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}` — or paste the value from Railway MySQL → Variables → MYSQL_PUBLIC_URL. |
| `JWT_SECRET` | Your own long secret, at least 64 characters. |
| `MAIL_USERNAME` | Your Gmail address, e.g. `you@gmail.com`. (optional) |
| `MAIL_PASSWORD` | Gmail App Password. (optional) |
| `MAIL_FROM` | Same as sender email. (optional) |
| `APP_FRONTEND_URL` | Your Vercel app URL with no trailing slash. |
| `STRIPE_SECRET_KEY` | `sk_live_...` from Stripe Dashboard. (optional) |

### Vercel (Frontend) → Environment Variables

| Variable (Name) | Value / What to put in the field | Environment |
|-----------------|----------------------------------|-------------|
| `VITE_API_URL` | `https://YOUR-RAILWAY-BACKEND.up.railway.app` — replace with your real Railway backend URL. No trailing slash. | Production (and Preview if needed) |
| `VITE_APP_ENV` | `production` | Production |
| `VITE_STRIPE_PUBLISHABLE_KEY` | `pk_live_...` from Stripe. (optional) | Production |

---

## 5. Current setup (template)

Replace placeholders with your real URLs and secrets.

### Railway (Backend) – Environment Variables (JSON)

```json
{
  "SPRING_PROFILES_ACTIVE": "prod",
  "MYSQL_PUBLIC_URL": "${{ YourMySQLServiceName.MYSQL_PUBLIC_URL }}",
  "JWT_SECRET": "YOUR_64_CHAR_SECRET_HERE",
  "APP_FRONTEND_URL": "https://your-loan-shark-app.vercel.app",
  "MAIL_USERNAME": "",
  "MAIL_PASSWORD": "",
  "MAIL_FROM": "",
  "STRIPE_SECRET_KEY": ""
}
```

### Vercel (Frontend) – Environment Variables (JSON)

```json
{
  "VITE_API_URL": "https://YOUR-RAILWAY-BACKEND.up.railway.app",
  "VITE_APP_ENV": "production",
  "VITE_STRIPE_PUBLISHABLE_KEY": ""
}
```
