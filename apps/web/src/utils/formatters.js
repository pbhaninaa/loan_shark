const currencyFormatter = new Intl.NumberFormat("en-ZA", {
  style: "currency",
  currency: "ZAR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

export function formatCurrency(value) {
  const numericValue = Number(value ?? 0);
  if (Number.isNaN(numericValue)) {
    return currencyFormatter.format(0);
  }
  return currencyFormatter.format(numericValue);
}

export function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : "-";
}

export function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : "-";
}
