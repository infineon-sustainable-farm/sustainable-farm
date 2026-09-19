import { useQuery } from "@tanstack/react-query";
import { fetchGrowthCalendar } from "../api/plantsApi";

function growthCalendarQueryKey({ bloc_parcelle = null, id_ferme = null } = {}) {
    return ["plants", "growth-calendar", { bloc_parcelle, id_ferme }];
}

/**
 * Growth calendar entries matching the given filters.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useGrowthCalendar({ bloc_parcelle = null, id_ferme = null } = {}) {
    return useQuery({
        queryKey: growthCalendarQueryKey({ bloc_parcelle, id_ferme }),
        queryFn: () => fetchGrowthCalendar({ bloc_parcelle, id_ferme }),
    });
}

/**
 * The distinct block values that actually exist in the growth calendar.
 *
 * Derived from the rows, never hardcoded. Shares its cache entry with an
 * unfiltered useGrowthCalendar() call.
 */
export function useGrowthCalendarBlocks() {
    return useQuery({
        queryKey: growthCalendarQueryKey(),
        queryFn: () => fetchGrowthCalendar(),
        select: (entries) =>
            [...new Set(entries.map((entry) => entry.bloc_parcelle).filter(Boolean))].sort(),
    });
}
