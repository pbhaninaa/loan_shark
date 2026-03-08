# BackEnd – database connection (Railway MySQL)

The BackEnd **must** receive MySQL connection details as **environment variables** on the service that runs the API.

---

## BackEnd deployed **on Railway**

Use the **private** URL (only works inside Railway):

| Variable | Value |
|----------|--------|
| `MYSQL_URL` | `${{ YourMySQLServiceName.MYSQL_URL }}` |

Replace `YourMySQLServiceName` with your MySQL service name (e.g. `MySQL-Q-C2`).

---

## BackEnd deployed **outside Railway** (e.g. Google Cloud Run)

The host `mysql-q-c2.railway.internal` **only resolves inside Railway**. You must use the **public** URL.

**Option A – Prefer public URL when both exist**

Set **`MYSQL_PUBLIC_URL`** on your BackEnd service to the **value** of Railway’s public URL:

1. In Railway → your **MySQL** service → **Variables**.
2. Copy the value of **`MYSQL_PUBLIC_URL`** (e.g. `mysql://root:xxx@gondola.proxy.rlwy.net:49401/railway`).
3. In your **BackEnd** deployment (e.g. Cloud Run → Variables), add:
   - **Name:** `MYSQL_PUBLIC_URL`
   - **Value:** (paste the copied string)

The app uses `MYSQL_PUBLIC_URL` when set, so it will connect via the public host.

**Option B – Use MYSQL_URL with public URL**

Set **`MYSQL_URL`** on the BackEnd to the same public URL string (again, copy the value from Railway MySQL → `MYSQL_PUBLIC_URL`). Do **not** use the private hostname (e.g. `*.railway.internal`) when the BackEnd runs outside Railway.

---

## After adding variables

Redeploy the BackEnd. The app will create a DataSource and connect to your Railway MySQL.
