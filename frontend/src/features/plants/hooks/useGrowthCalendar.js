import { useQuery } from "@tanstack/react-query";
import { fetchGrowthCalendar } from "../api/plantsApi";

function growthCalendarQueryKey({ blockCode = null, farmId = null } = {}) {
    return ["plants", "growth-calendar", { blockCode, farmId }];
}

/**
 * Growth calendar entries matching the given filters.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useGrowthCalendar({ blockCode = null, farmId = null } = {}) {
    return useQuery({
        queryKey: growthCalendarQueryKey({ blockCode, farmId }),
        queryFn: () => fetchGrowthCalendar({ blockCode, farmId }),
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
            [...new Set(entries.map((entry) => entry.blockCode).filter(Boolean))].sort(),
    });
}
