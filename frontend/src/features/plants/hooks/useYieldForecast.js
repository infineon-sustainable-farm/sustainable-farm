import { useQuery } from "@tanstack/react-query";
import { fetchYieldForecast } from "../api/plantsApi";

function yieldForecastQueryKey({ blockCode = null, farmId = null, from = null, months = null } = {}) {
    return ["plants", "yield-forecast", { blockCode, farmId, from, months }];
}

/**
 * Expected yield by month for the given filters and period.
 *
 * A null `from` lets the API start at its current month; the month it chose
 * comes back in the response. Loading, error and retry are handled by TanStack
 * Query; the components read its status flags instead of keeping their own.
 */
export function useYieldForecast({ blockCode = null, farmId = null, from = null, months = null } = {}) {
    return useQuery({
        queryKey: yieldForecastQueryKey({ blockCode, farmId, from, months }),
        queryFn: () => fetchYieldForecast({ blockCode, farmId, from, months }),
    });
}
