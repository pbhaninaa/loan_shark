const BACKEND_UNREACHABLE_MESSAGE =
  "Backend is unreachable. Check that the API is running and that VITE_API_URL (or the connection URL) is correct.";

function isBackendUnreachable(err) {
  if (!err) return false;
  if (err.response) return false;
  const code = err.code;
  const msg = (err.message || "").toLowerCase();
  return (
    code === "ERR_NETWORK" ||
    code === "ERR_CONNECTION_REFUSED" ||
    msg.includes("network") ||
    msg.includes("failed to fetch")
  );
}

export function extractApiError(requestError, fallbackMessage = "Request failed") {
  if (isBackendUnreachable(requestError)) {
    return BACKEND_UNREACHABLE_MESSAGE;
  }
  const responseData = requestError?.response?.data;
  if (!responseData) {
    return requestError?.message || fallbackMessage;
  }

  if (typeof responseData.message === "string" && responseData.message.trim()) {
    return responseData.message;
  }

  if (responseData.details && typeof responseData.details === "object") {
    const entries = Object.entries(responseData.details)
      .filter(([, value]) => value)
      .map(([field, value]) => `${field}: ${value}`);
    if (entries.length) {
      return entries.join("; ");
    }
  }

  return fallbackMessage;
}
