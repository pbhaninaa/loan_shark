export function extractApiError(requestError, fallbackMessage = "Request failed") {
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
