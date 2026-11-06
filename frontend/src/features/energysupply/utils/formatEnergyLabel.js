export function formatEnergyLabel(value) {
  if (!value) return "—";

  return String(value)
    .toLowerCase()
    .split("_")
    .map((word) => word === "pv" ? "PV" : word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}
