import { useQuery } from "@tanstack/react-query";
import { fetchHarvestStatus } from "../api/plantsApi";

function harvestStatusQueryKey({ blockCode = null, farmId = null, withinDays = null } = {}) {
    return ["plants", "harvest-status", { blockCode, farmId, withinDays }];
}

/**
 * The maturity status of the day of the planted varieties matching the given
 * filters, in the order of the API: by block, then by variety, with the day
 * the API computed them for.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useHarvestStatus({ blockCode = null, farmId = null, withinDays = null } = {}) {
    return useQuery({
        queryKey: harvestStatusQueryKey({ blockCode, farmId, withinDays }),
        queryFn: () => fetchHarvestStatus({ blockCode, farmId, withinDays }),
    });
}
