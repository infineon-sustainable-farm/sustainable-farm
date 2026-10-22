import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFertilizer } from "../api/plantsApi";

/**
 * Adds a fertilizer to the catalogue.
 *
 * Only the catalogue is refreshed afterwards: the API writes the fertilizer
 * alone, with no movement.
 */
export function useAddFertilizer() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createFertilizer,
        onSettled: () => queryClient.invalidateQueries({ queryKey: ["plants", "fertilizers"] }),
    });
}
