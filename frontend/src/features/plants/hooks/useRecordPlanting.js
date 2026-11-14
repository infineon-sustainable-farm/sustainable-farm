import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createPlanting } from "../api/plantsApi";

/**
 * What a planting changes, whatever the filter of each query: a planting can
 * add a variety row, change a current tree count, move the planting date of a
 * block and add trees to the forecast, the yield comparison and the harvest
 * status. The alerts follow the forecast: it decides when a harvest season
 * opens, and so whether a pre-harvest interval is critical. The overview
 * sums up the trees, their maturity status and the alerts. A nursery
 * transplant is a planting too, and refreshes the same queries.
 */
export const PLANTING_QUERY_KEYS = [
    ["plants", "varieties"],
    ["plants", "growth-calendar"],
    ["plants", "yield-forecast"],
    ["plants", "yield-comparison"],
    ["plants", "harvest-status"],
    ["plants", "alerts"],
    ["plants", "overview"],
];

/** Records a planting, then refreshes every query in PLANTING_QUERY_KEYS. */
export function useRecordPlanting() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createPlanting,
        onSettled: () =>
            Promise.all(PLANTING_QUERY_KEYS.map((queryKey) => queryClient.invalidateQueries({ queryKey }))),
    });
}
