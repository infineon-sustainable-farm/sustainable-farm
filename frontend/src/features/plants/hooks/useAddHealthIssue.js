import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createHealthIssue } from "../api/plantsApi";

/**
 * Adds a pest or a disease to the catalogue.
 *
 * Only the catalogue is refreshed afterwards: the API writes the issue alone.
 */
export function useAddHealthIssue() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createHealthIssue,
        onSettled: () => queryClient.invalidateQueries({ queryKey: ["plants", "health-issues"] }),
    });
}
