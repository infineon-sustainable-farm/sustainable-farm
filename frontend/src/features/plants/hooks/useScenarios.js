import { useQuery } from "@tanstack/react-query";
import { fetchScenarios } from "../api/plantsApi";

/**
 * The planting scenarios, in the order of the API: by name. Outside the dev
 * profile the list starts empty, until the user enters a baseline.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useScenarios() {
    return useQuery({
        queryKey: ["plants", "scenarios"],
        queryFn: fetchScenarios,
    });
}
