import { useQuery } from "@tanstack/react-query";
import { fetchPlantAlerts } from "../api/plantsApi";

/** The alert type about the nursery; health, harvest and stock alerts have their own screens. */
const NURSERY_ALERT_TYPE = "NURSERY_READY";

/**
 * The batches to transplant today, for every block: those ready to transplant
 * and those whose planned transplant date has come, in the order of the API,
 * with the day the API computed them for.
 *
 * The API computes every alert; only the type is picked here. The cache entry
 * is the one of the full list, which the health alerts read too.
 */
export function useNurseryAlerts() {
    return useQuery({
        queryKey: ["plants", "alerts", { blockCode: null, farmId: null, withinDays: null }],
        queryFn: () => fetchPlantAlerts(),
        select: (response) => ({
            today: response.today,
            alerts: response.alerts.filter((alert) => alert.type === NURSERY_ALERT_TYPE),
        }),
    });
}
