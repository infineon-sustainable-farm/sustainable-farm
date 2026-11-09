import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFertilizer } from "../api/plantsApi";

/**
 * Adds a fertilizer to the catalogue.
 *
 * The catalogue, the plant alerts and the overview are refreshed afterwards:
 * the API writes the fertilizer alone, with no movement, but a fertilizer
 * entered with an alert threshold has no stock yet, and so is a stock alert at
 * once.
 */
export function useAddFertilizer() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createFertilizer,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizers"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
            ]),
    });
}
