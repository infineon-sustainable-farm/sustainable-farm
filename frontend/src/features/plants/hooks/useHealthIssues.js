import { useQuery } from "@tanstack/react-query";
import { fetchHealthIssues } from "../api/plantsApi";

/**
 * The catalogue of pests and diseases, in the order of the API: the pests,
 * then the diseases, then "Other", each by name.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useHealthIssues() {
    return useQuery({
        queryKey: ["plants", "health-issues"],
        queryFn: fetchHealthIssues,
    });
}
