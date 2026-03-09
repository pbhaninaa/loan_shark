import axios from "axios";
import { isTokenExpired } from "../utils/token";

// Build-time: VITE_API_URL (set on Vercel). Runtime fallback: if not on localhost, use Railway so deployed app works.
const RAILWAY_API_URL = "https://backend-production-8d8d.up.railway.app";
const buildTimeApiUrl = import.meta.env.VITE_API_URL;
const isDeployed = typeof window !== "undefined" && !/localhost|127\.0\.0\.1/.test(window.location?.hostname || "");
const defaultApiUrl = isDeployed ? RAILWAY_API_URL : "http://localhost:8080";
const baseURL = buildTimeApiUrl || defaultApiUrl;

const api = axios.create({
  baseURL,
  headers: { Accept: "application/json" }
});

// Force deployed app to use Railway: if we're not on localhost but baseURL is localhost (e.g. old cache), fix it per request
api.interceptors.request.use((config) => {
  const onDeployedHost = typeof window !== "undefined" && !/localhost|127\.0\.0\.1/.test(window.location?.hostname || "");
  const pointingToLocal = !config.baseURL || /localhost|127\.0\.0\.1/.test(config.baseURL || "");
  if (onDeployedHost && pointingToLocal) {
    config.baseURL = RAILWAY_API_URL;
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    if (response.config?.skipGlobalToast) return response;
    const method = response.config?.method?.toUpperCase();
    const isMutation = ["POST", "PUT", "PATCH", "DELETE"].includes(method);
    const message = response.data?.message;
    if (isMutation && message) {
      toast.success(message);
    }
    return response;
  },
  (error) => {
    const isUnreachable =
      !error.response &&
      (error.code === "ERR_NETWORK" ||
        error.code === "ERR_CONNECTION_REFUSED" ||
        error.message === "Network Error" ||
        error.message?.toLowerCase?.().includes("network") ||
        error.message?.toLowerCase?.().includes("failed to fetch"));
    const isTimeout = error.code === "ECONNABORTED";

    if (isTimeout) {
      error.message = "Request timeout. The backend took too long to respond. Check your connection.";
    } else if (isUnreachable) {
      error.message = "Server is down, please try again later.";
    }

    if (!error.config?.skipGlobalToast && typeof toast !== "undefined") {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        "An unexpected error occurred. Please try again later.";
      toast.error(message);
    }
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || "";
      if (!requestUrl.includes("/auth/login")) {
        window.dispatchEvent(new CustomEvent("auth-logout", { detail: { reason: "401" } }));
        window.location.hash = "#/login";
      }
    }

    return Promise.reject(error);
  }
);

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("loanSharkToken");
  // Use baseURL-relative path so matching works (axios may provide full URL)
  const url = config.url || "";
  const path = url.replace(config.baseURL || "", "").split("?")[0];
  const isPublic =
    path.startsWith("/auth/login") ||
    path.startsWith("/auth/register/owner") ||
    path.startsWith("/auth/register/borrower") ||
    path.startsWith("/auth/setup-status") ||
    path.startsWith("/auth/forgot-password") ||
    path.startsWith("/auth/reset-password") ||
    ((config.method || "").toLowerCase() === "get" && path === "/settings/loan-interest");

  // If token is expired, log out and redirect before sending request
  if (token && !isPublic && isTokenExpired(token)) {
    window.dispatchEvent(new CustomEvent("auth-logout", { detail: { reason: "expired" } }));
    window.location.hash = "#/login";
    return Promise.reject(new Error("Session expired. Please log in again."));
  }

  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
