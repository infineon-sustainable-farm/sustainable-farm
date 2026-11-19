import { useQuery } from "@tanstack/react-query";
import { fetchBlockHealth } from "../api/plantsApi";

function blockHealthQueryKey({ farmId = null } = {}) {
    return ["plants", "block-health", { farmId }];
}

/**
 * The current health of each inspected block, in the order of the API: by
 * block. Score, category and inspection date all come from the API.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useBlockHealth({ farmId = null } = {}) {
    return useQuery({
        queryKey: blockHealthQueryKey({ farmId }),
        queryFn: () => fetchBlockHealth({ farmId }),
    });
}

/**
 * The distinct blocks that have been inspected. An inspection needs no
 * planting, so some of them may have no variety. Shares its cache entry with
 * an unfiltered useBlockHealth() call.
 */
export function useInspectedBlocks() {
    return useQuery({
        queryKey: blockHealthQueryKey(),
        queryFn: () => fetchBlockHealth(),
        select: (blocks) => [...new Set(blocks.map((block) => block.blockCode).filter(Boolean))].sort(),
    });
}
