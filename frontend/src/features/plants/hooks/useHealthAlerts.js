import { useQuery } from "@tanstack/react-query";
import { fetchPlantAlerts } from "../api/plantsApi";

/** The alert types about plant health; harvest seasons, stocks and nursery batches have their own screens. */
const HEALTH_ALERT_TYPES = ["OPEN_HEALTH_ISSUE", "PRE_HARVEST_INTERVAL"];

/**
 * The health alerts of the day, for every block: open health problems and
 * pre-harvest intervals, in the order of the API (CRITICAL first, then by
 * date), with the day the API computed them for.
 *
 * The API computes every alert; only the types are picked here. The cache
 * entry is the one of the full list, so another screen can reuse it.
 */
export function useHealthAlerts() {
    return useQuery({
        queryKey: ["plants", "alerts", { blockCode: null, farmId: null, withinDays: null }],
        queryFn: () => fetchPlantAlerts(),
        select: (response) => ({
            today: response.today,
            alerts: response.alerts.filter((alert) => HEALTH_ALERT_TYPES.includes(alert.type)),
        }),
    });
}
