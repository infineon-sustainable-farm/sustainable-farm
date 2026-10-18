import { useQuery } from "@tanstack/react-query";
import { fetchVarieties } from "../api/plantsApi";

function varietiesQueryKey({ blockCode = null, farmId = null } = {}) {
    return ["plants", "varieties", { blockCode, farmId }];
}

/**
 * Varieties matching the given filters.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useVarieties({ blockCode = null, farmId = null } = {}) {
    return useQuery({
        queryKey: varietiesQueryKey({ blockCode, farmId }),
        queryFn: () => fetchVarieties({ blockCode, farmId }),
    });
}

/**
 * The distinct block values that actually exist in the data.
 *
 * The filter options are derived from the rows rather than hardcoded, so the
 * dropdown never offers a block the database does not contain. Shares its cache
 * entry with an unfiltered useVarieties() call.
 */
export function useVarietyBlocks() {
    return useQuery({
        queryKey: varietiesQueryKey(),
        queryFn: () => fetchVarieties(),
        select: (varieties) =>
            [...new Set(varieties.map((variety) => variety.blockCode).filter(Boolean))].sort(),
    });
}
