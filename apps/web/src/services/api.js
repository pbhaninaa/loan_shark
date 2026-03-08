import axios from "axios";

// Use Railway (or other) backend URL in production; localhost when developing
const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL
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
      error.message = "Backend is unreachable. Check that the API is running and that VITE_API_URL (or the connection URL) is correct.";
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
        localStorage.removeItem("loanSharkToken");
        localStorage.removeItem("loanSharkRole");
        localStorage.removeItem("loanSharkUserId");
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
    ((config.method || "").toLowerCase() === "get" && path.startsWith("/settings/loan-interest"));

  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
