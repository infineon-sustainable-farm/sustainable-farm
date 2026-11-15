import { useQuery } from "@tanstack/react-query";
import { fetchYieldComparison } from "../api/plantsApi";

function yieldComparisonQueryKey({ blockCode = null, farmId = null, from = null, months = null } = {}) {
    return ["plants", "yield-comparison", { blockCode, farmId, from, months }];
}

/**
 * Actual against expected yield for the given filters and period.
 *
 * A null `from` lets the API start at January of its current year; the month
 * it chose comes back in the response. Loading, error and retry are handled by
 * TanStack Query; the components read its status flags instead of keeping
 * their own.
 */
export function useYieldComparison({ blockCode = null, farmId = null, from = null, months = null } = {}) {
    return useQuery({
        queryKey: yieldComparisonQueryKey({ blockCode, farmId, from, months }),
        queryFn: () => fetchYieldComparison({ blockCode, farmId, from, months }),
    });
}
