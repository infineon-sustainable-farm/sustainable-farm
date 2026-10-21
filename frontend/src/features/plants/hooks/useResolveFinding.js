import { useMutation, useQueryClient } from "@tanstack/react-query";
import { resolveHealthFinding } from "../api/plantsApi";

/**
 * Resolves a finding, given as `{ findingId, resolution }`.
 *
 * Every findings list and the plant alerts are refreshed afterwards, whatever
 * their filter, and even when the API refuses: a 409 means the finding was
 * resolved elsewhere, and its row must show it. A resolved finding is no
 * longer an open problem.
 */
export function useResolveFinding() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ findingId, resolution }) => resolveHealthFinding(findingId, resolution),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "health-findings"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
            ]),
    });
}
