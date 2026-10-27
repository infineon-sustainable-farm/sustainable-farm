import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createMaturityObservation } from "../api/plantsApi";

/**
 * Records a maturity observation.
 *
 * The harvest status and the overview are refreshed afterwards, whatever their
 * filter: there the observation of the season under way takes precedence over
 * the estimated status, and the overview counts the status retained, in Ready
 * for Harvest and in its ring. Nothing else depends on it: the alerts and the
 * forecast follow the estimate, the harvests and the yield comparison the
 * recorded harvests.
 */
export function useRecordMaturityObservation() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createMaturityObservation,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "harvest-status"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
            ]),
    });
}
