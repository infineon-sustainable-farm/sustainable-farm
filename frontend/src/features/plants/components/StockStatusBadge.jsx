import PlantsBadge from "./PlantsBadge";

/**
 * Stock status of a fertilizer, as decided by the API: "Low stock" when the
 * stock is at or below the alert threshold, nothing otherwise. No stock or
 * threshold is compared here.
 */
export default function StockStatusBadge({ belowThreshold }) {
    if (!belowThreshold) return null;
    return <PlantsBadge tone="error">Low stock</PlantsBadge>;
}
