import { useQuery } from "@tanstack/react-query";
import { fetchEurXofRate } from "../api/plantsApi";

/**
 * How many FCFA one euro is worth, or null while no rate is recorded.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useEurXofRate() {
    return useQuery({
        queryKey: ["plants", "eur-xof-rate"],
        queryFn: fetchEurXofRate,
    });
}
