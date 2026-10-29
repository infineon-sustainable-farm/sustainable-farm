export function formatDecimal(value, fractionDigits = 2) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  return amount.toFixed(fractionDigits);
}

export function formatCurrency(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  return `$${amount.toFixed(2)}`;
}
