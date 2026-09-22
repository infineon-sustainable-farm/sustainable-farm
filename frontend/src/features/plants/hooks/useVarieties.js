import { useQuery } from "@tanstack/react-query";
import { fetchVarieties } from "../api/plantsApi";

function varietiesQueryKey({ bloc_parcelle = null, id_ferme = null } = {}) {
    return ["plants", "varieties", { bloc_parcelle, id_ferme }];
}

/**
 * Varieties matching the given filters.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useVarieties({ bloc_parcelle = null, id_ferme = null } = {}) {
    return useQuery({
        queryKey: varietiesQueryKey({ bloc_parcelle, id_ferme }),
        queryFn: () => fetchVarieties({ bloc_parcelle, id_ferme }),
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
            [...new Set(varieties.map((variety) => variety.bloc_parcelle).filter(Boolean))].sort(),
    });
}
