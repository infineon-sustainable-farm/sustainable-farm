import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createNurseryBatch } from "../api/plantsApi";

/**
 * Starts a nursery batch.
 *
 * The batches, every history list and the plant alerts are refreshed
 * afterwards: the API records the initial stage as the batch's first event,
 * and a batch started ready to transplant, or whose planned date has come, is
 * an alert at once.
 */
export function useAddNurseryBatch() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createNurseryBatch,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "nursery-batches"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "nursery-events"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
            ]),
    });
}
