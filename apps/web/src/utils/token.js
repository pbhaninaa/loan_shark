/**
 * Decode JWT payload and check if the token has expired.
 * @param {string} token - JWT token
 * @param {number} bufferSeconds - Consider expired this many seconds before actual expiry (default 30)
 * @returns {boolean} true if token is expired or invalid
 */
export function isTokenExpired(token, bufferSeconds = 30) {
  if (!token || typeof token !== "string") return true;
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return true;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")));
    const exp = payload.exp;
    if (exp == null || typeof exp !== "number") return false; // no exp claim = assume valid
    const now = Math.floor(Date.now() / 1000);
    return exp - bufferSeconds <= now;
  } catch {
    return true;
  }
}
