import { useQuery } from "@tanstack/react-query";
import { fetchVarietyReferences } from "../api/plantsApi";

/**
 * The reference of every variety, in the order of the API: by name.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useVarietyReferences() {
    return useQuery({
        queryKey: ["plants", "variety-references"],
        queryFn: fetchVarietyReferences,
    });
}
