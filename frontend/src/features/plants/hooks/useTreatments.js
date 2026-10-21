import { useQuery } from "@tanstack/react-query";
import { fetchTreatments } from "../api/plantsApi";

function treatmentsQueryKey({ blockCode = null, farmId = null, from = null, to = null } = {}) {
    return ["plants", "treatments", { blockCode, farmId, from, to }];
}

/**
 * The treatment record matching the given filters, in the order of the API:
 * by treatment date, then by record.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useTreatments({ blockCode = null, farmId = null, from = null, to = null } = {}) {
    return useQuery({
        queryKey: treatmentsQueryKey({ blockCode, farmId, from, to }),
        queryFn: () => fetchTreatments({ blockCode, farmId, from, to }),
    });
}

/**
 * The distinct blocks that have been treated. A preventive treatment needs no
 * planting and no inspection, so some of them may be known from it alone.
 * Shares its cache entry with an unfiltered useTreatments() call.
 */
export function useTreatedBlocks() {
    return useQuery({
        queryKey: treatmentsQueryKey(),
        queryFn: () => fetchTreatments(),
        select: (treatments) =>
            [...new Set(treatments.map((treatment) => treatment.blockCode).filter(Boolean))].sort(),
    });
}
