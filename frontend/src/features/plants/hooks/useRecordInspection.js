import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createHealthInspection } from "../api/plantsApi";

/**
 * Records an inspection with its findings.
 *
 * The health of the blocks, every findings list, the plant alerts and the
 * overview are refreshed afterwards, whatever their filter: an inspection can
 * change the latest score of its block, and each finding is an open problem,
 * counted by the overview. The catalogue is refreshed too, as the API adds
 * "Other" the first time it is used.
 */
export function useRecordInspection() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createHealthInspection,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "block-health"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "health-findings"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "health-issues"] }),
            ]),
    });
}
