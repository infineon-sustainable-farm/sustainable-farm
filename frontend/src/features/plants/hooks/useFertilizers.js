import { useQuery } from "@tanstack/react-query";
import { fetchFertilizers } from "../api/plantsApi";

/**
 * The fertilizer catalogue, each fertilizer with its current stock, in the
 * order of the API: by name.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useFertilizers() {
    return useQuery({
        queryKey: ["plants", "fertilizers"],
        queryFn: fetchFertilizers,
    });
}
