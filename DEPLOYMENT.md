# What to set where – Railway (Backend + DB) and Vercel (Frontend)

Same deployment pattern as **Mechanic-Management**: Railway for backend + MySQL, Vercel for frontend.

---

## Backend won’t start: “Failed to configure a DataSource”

DB is configured **only via properties** (like Mechanic Management). Set these on the Backend service in Railway:

- **SPRING_DATASOURCE_URL** – JDBC URL, e.g. `jdbc:mysql://host:port/railway?useSSL=true`. Use the **public** host from your Railway MySQL service (e.g. `roundhouse.proxy.rlwy.net:12345`), not the private `*.railway.internal` host.
- **SPRING_DATASOURCE_USERNAME** – e.g. from MySQL service Variables (often `root`).
- **SPRING_DATASOURCE_PASSWORD** – from MySQL service Variables.

Set **SPRING_PROFILES_ACTIVE** to exactly one value: `prod`, `uat`, or `sit`.

---

## 1. Railway (Backend + Database)

**Repo layout:** Backend lives in **`services/api`**. In Railway, set **Root Directory** to **`services/api`** (Dockerfile there).

DB is configured only via **properties / env** (same as Mechanic Management). In your **Railway project** (backend service), open **Variables** and set:

### Required

| Variable | Where to get it | Example |
|----------|-----------------|---------|
| `SPRING_DATASOURCE_URL` | JDBC URL. From Railway MySQL use the **public** URL: convert `mysql://user:pass@host:port/db` to `jdbc:mysql://host:port/db?useSSL=true` (same host/port as in MySQL service Variables). | `jdbc:mysql://roundhouse.proxy.rlwy.net:12345/railway?useSSL=true` |
| `SPRING_DATASOURCE_USERNAME` | Railway MySQL → Variables (e.g. `root`) | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Railway MySQL → Variables | (value from MySQL service) |
| `JWT_SECRET` | Generate a long random string (min 64 chars). E.g. `openssl rand -base64 48` | `your_64_char_secret_...` |

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
- [ ] `SPRING_DATASOURCE_URL` = JDBC URL (public MySQL host, e.g. `jdbc:mysql://host:port/db?useSSL=true`)
- [ ] `SPRING_DATASOURCE_USERNAME` = MySQL username (e.g. from MySQL service)
- [ ] `SPRING_DATASOURCE_PASSWORD` = MySQL password (from MySQL service)
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
| `SPRING_DATASOURCE_URL` | JDBC URL, e.g. `jdbc:mysql://YOUR_PUBLIC_MYSQL_HOST:PORT/railway?useSSL=true` (use public host from MySQL service). |
| `SPRING_DATASOURCE_USERNAME` | MySQL username (e.g. `root` from MySQL service). |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password (from MySQL service Variables). |
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
