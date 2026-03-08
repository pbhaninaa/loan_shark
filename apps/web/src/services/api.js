import axios from "axios";

// Use Railway (or other) backend URL in production; localhost when developing
const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL
});

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
    ((config.method || "").toLowerCase() === "get" && path.startsWith("/settings/loan-interest"));

  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
