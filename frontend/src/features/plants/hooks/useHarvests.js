import { useQuery } from "@tanstack/react-query";
import { fetchHarvests } from "../api/plantsApi";

function harvestsQueryKey({ blockCode = null, farmId = null, from = null, to = null } = {}) {
    return ["plants", "harvests", { blockCode, farmId, from, to }];
}

/**
 * Recorded harvests matching the given filters, in the order of the API: by
 * harvest date, then by record.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useHarvests({ blockCode = null, farmId = null, from = null, to = null } = {}) {
    return useQuery({
        queryKey: harvestsQueryKey({ blockCode, farmId, from, to }),
        queryFn: () => fetchHarvests({ blockCode, farmId, from, to }),
    });
}
