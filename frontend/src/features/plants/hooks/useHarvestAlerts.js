import { useQuery } from "@tanstack/react-query";
import { fetchPlantAlerts } from "../api/plantsApi";

/** The alert types about harvesting; health, stock and nursery alerts have their own screens. */
const HARVEST_ALERT_TYPES = ["HARVEST_APPROACHING", "PRE_HARVEST_INTERVAL"];

/**
 * The harvest deadlines of the day for the given block, or for every block:
 * the harvest seasons in progress or opening soon, and the pre-harvest
 * intervals still running, in the order of the API (CRITICAL first, then by
 * date), with the day and the look-ahead the API computed them with.
 *
 * The API computes every alert; only the types are picked here. Without a
 * block, the cache entry is the one of the full list, which the health and
 * nursery alerts read too.
 */
export function useHarvestAlerts({ blockCode = null } = {}) {
    return useQuery({
        queryKey: ["plants", "alerts", { blockCode, farmId: null, withinDays: null }],
        queryFn: () => fetchPlantAlerts({ blockCode }),
        select: (response) => ({
            today: response.today,
            withinDays: response.withinDays,
            alerts: response.alerts.filter((alert) => HARVEST_ALERT_TYPES.includes(alert.type)),
        }),
    });
}
