# Railway BackEnd – database connection

The BackEnd **must** receive MySQL connection details as **environment variables**. Add them to the **BackEnd** service (the one that runs the API), not only to the MySQL service.

## Option 1: Single variable (recommended)

In **BackEnd** → **Variables** → **New Variable**:

| Variable | Value |
|----------|--------|
| `MYSQL_URL` | `${{ YourMySQLServiceName.MYSQL_URL }}` |

Replace `YourMySQLServiceName` with your MySQL service name (e.g. `MySQL-Q-C2`).

## Option 2: Separate variables

If you prefer to reference each field:

| Variable | Value |
|----------|--------|
| `MYSQLHOST` | `${{ YourMySQLServiceName.MYSQLHOST }}` |
| `MYSQLPORT` | `${{ YourMySQLServiceName.MYSQLPORT }}` |
| `MYSQLUSER` | `${{ YourMySQLServiceName.MYSQLUSER }}` |
| `MYSQLPASSWORD` | `${{ YourMySQLServiceName.MYSQLPASSWORD }}` |
| `MYSQLDATABASE` | `${{ YourMySQLServiceName.MYSQLDATABASE }}` |

## After adding variables

Redeploy the BackEnd. The app will create a DataSource from `MYSQL_URL` (or from the separate vars) and connect to your Railway MySQL.
