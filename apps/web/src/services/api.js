import axios from "axios";
import { toast } from "vue-sonner";
import { isTokenExpired } from "../utils/token";

// ------------------------------
// Base URL
// ------------------------------
// Vercel environment variable (build-time)
const baseURL = import.meta.env.VITE_API_URL;
console.log("BASE URL:", baseURL);
if (!baseURL) {
  throw new Error("VITE_API_URL is not set");
}

// ------------------------------
// Axios instance
// ------------------------------
const api = axios.create({
  baseURL,
  headers: { Accept: "application/json" },
});

// ------------------------------
// REQUEST INTERCEPTOR (Auth + Localhost safety)
// ------------------------------
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("loanSharkToken");
  const url = config.url || "";
  const path = url.replace(config.baseURL || "", "").split("?")[0];

  const isPublic =
    path.startsWith("/auth/login") ||
    path.startsWith("/auth/register/owner") ||
    path.startsWith("/auth/register/borrower") ||
    path.startsWith("/auth/setup-status") ||
    path.startsWith("/auth/forgot-password") ||
    path.startsWith("/auth/reset-password") ||
    ((config.method || "").toLowerCase() === "get" &&
      path === "/settings/loan-interest");

  // Auto logout if token expired
  if (token && !isPublic && isTokenExpired(token)) {
    window.dispatchEvent(
      new CustomEvent("auth-logout", { detail: { reason: "expired" } })
    );
    window.location.hash = "#/login";
    return Promise.reject(new Error("Session expired. Please log in again."));
  }

  // Add Authorization header
  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// ------------------------------
// RESPONSE INTERCEPTOR (Global toast + error handling)
// ------------------------------
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
      ["ERR_NETWORK", "ERR_CONNECTION_REFUSED"].includes(error.code) ||
      error.message?.toLowerCase().includes("network") ||
      error.message?.toLowerCase().includes("failed to fetch");

    const isTimeout = error.code === "ECONNABORTED";

    if (isTimeout) {
      error.message =
        "Request timeout. The backend took too long to respond. Check your connection.";
    } else if (isUnreachable) {
      error.message = "Server is down, please try again later.";
    }

    if (!error.config?.skipGlobalToast) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        "An unexpected error occurred. Please try again later.";

      toast.error(message);
    }

    // Auto logout on 401
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || "";
      if (!requestUrl.includes("/auth/login")) {
        window.dispatchEvent(
          new CustomEvent("auth-logout", { detail: { reason: "401" } })
        );
        window.location.hash = "#/login";
      }
    }

    return Promise.reject(error);
  }
);

export default api;