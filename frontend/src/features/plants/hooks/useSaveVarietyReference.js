import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createVarietyReference, updateVarietyReference } from "../api/plantsApi";

/**
 * Enters the reference of a variety when `id` is null, or corrects the
 * reference `id`.
 *
 * The references, every forecast and yield comparison, the harvest status,
 * the plant alerts and the overview are refreshed afterwards: the API reads
 * the reference on every one of them, so the expected yields, the harvest
 * windows, the maturity statuses and the harvest season alerts change with it.
 */
export function useSaveVarietyReference() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, reference }) =>
            id === null ? createVarietyReference(reference) : updateVarietyReference(id, reference),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "variety-references"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-forecast"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-comparison"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "harvest-status"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
            ]),
    });
}
