import { useQuery } from "@tanstack/react-query";
import { fetchFertilizerMovements } from "../api/plantsApi";

function fertilizerMovementsQueryKey({
    fertilizerId = null,
    movementType = null,
    blockCode = null,
    from = null,
    to = null,
} = {}) {
    return ["plants", "fertilizer-movements", { fertilizerId, movementType, blockCode, from, to }];
}

/**
 * Fertilizer movements matching the given filters, in the order of the API:
 * by movement date, then by record.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useFertilizerMovements({
    fertilizerId = null,
    movementType = null,
    blockCode = null,
    from = null,
    to = null,
} = {}) {
    return useQuery({
        queryKey: fertilizerMovementsQueryKey({ fertilizerId, movementType, blockCode, from, to }),
        queryFn: () => fetchFertilizerMovements({ fertilizerId, movementType, blockCode, from, to }),
    });
}

/**
 * The distinct blocks that recorded applications were made on.
 *
 * Derived from the movements, never hardcoded, so the filter never offers a
 * block no application names. Shares its cache entry with an unfiltered
 * useFertilizerMovements() call.
 */
export function useFertilizerMovementBlocks() {
    return useQuery({
        queryKey: fertilizerMovementsQueryKey(),
        queryFn: () => fetchFertilizerMovements(),
        select: (movements) =>
            [...new Set(movements.map((movement) => movement.blockCode).filter(Boolean))].sort(),
    });
}
