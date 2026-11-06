import { useQuery } from "@tanstack/react-query";
import { fetchPlantsOverview } from "../api/plantsApi";

function plantsOverviewQueryKey({ blockCode = null, farmId = null } = {}) {
    return ["plants", "overview", { blockCode, farmId }];
}

/**
 * The figures of the dashboard of the day for the given filters, with the day
 * the API computed them for: the trees, those in growth stage and ready for
 * harvest, the alerts and the critical ones, and the trees by maturity status.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function usePlantsOverview({ blockCode = null, farmId = null } = {}) {
    return useQuery({
        queryKey: plantsOverviewQueryKey({ blockCode, farmId }),
        queryFn: () => fetchPlantsOverview({ blockCode, farmId }),
    });
}
