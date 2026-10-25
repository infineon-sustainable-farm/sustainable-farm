import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createMaturityObservation } from "../api/plantsApi";

/**
 * Records a maturity observation.
 *
 * The harvest status is refreshed afterwards, whatever its filter: there the
 * observation of the season under way takes precedence over the estimated
 * status. Nothing else depends on it: the alerts and the forecast follow the
 * estimate, the harvests and the yield comparison the recorded harvests.
 */
export function useRecordMaturityObservation() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createMaturityObservation,
        onSettled: () => queryClient.invalidateQueries({ queryKey: ["plants", "harvest-status"] }),
    });
}
