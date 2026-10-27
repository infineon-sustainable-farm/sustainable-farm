import { useQuery } from "@tanstack/react-query";
import { fetchPlantAlerts } from "../api/plantsApi";

/**
 * Every plant alert of the day for the given block, or for every block, in
 * the order of the API (CRITICAL first, then by date), with the day the API
 * computed them for. With a block, the API leaves out the stock alerts: a
 * fertilizer belongs to no block.
 *
 * Nothing is picked here. The cache entry is the one the harvest, health and
 * nursery alerts read with their own types.
 */
export function usePlantAlerts({ blockCode = null } = {}) {
    return useQuery({
        queryKey: ["plants", "alerts", { blockCode, farmId: null, withinDays: null }],
        queryFn: () => fetchPlantAlerts({ blockCode }),
    });
}
