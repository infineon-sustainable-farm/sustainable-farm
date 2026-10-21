import { useQuery } from "@tanstack/react-query";
import { fetchNurseryBatches } from "../api/plantsApi";

const NURSERY_BATCHES_QUERY_KEY = ["plants", "nursery-batches"];

/**
 * The nursery batches, in the order of the API: by start date. The plants
 * left, the survival rate and the current stage come computed by the API.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useNurseryBatches() {
    return useQuery({
        queryKey: NURSERY_BATCHES_QUERY_KEY,
        queryFn: fetchNurseryBatches,
    });
}

/**
 * The distinct blocks the batches are planned to be transplanted to.
 *
 * Derived from the rows, never hardcoded, so the filter never offers a block
 * no batch names. Shares its cache entry with useNurseryBatches().
 */
export function useNurseryPlannedBlocks() {
    return useQuery({
        queryKey: NURSERY_BATCHES_QUERY_KEY,
        queryFn: fetchNurseryBatches,
        select: (batches) =>
            [...new Set(batches.map((batch) => batch.plannedBlockCode).filter(Boolean))].sort(),
    });
}
