import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createVarietyReference, updateVarietyReference } from "../api/plantsApi";

/**
 * Enters the reference of a variety when `id` is null, or corrects the
 * reference `id`.
 *
 * The references and every forecast are refreshed afterwards: the API reads
 * the reference on every forecast, so the expected yields change with it.
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
            ]),
    });
}
