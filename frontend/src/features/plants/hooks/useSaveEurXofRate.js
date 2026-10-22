import { useMutation, useQueryClient } from "@tanstack/react-query";
import { saveEurXofRate } from "../api/plantsApi";

/**
 * Enters the EUR to XOF rate, or replaces it.
 *
 * The rate and every movement list are refreshed afterwards: the API converts
 * the costs with the rate on every read, so the amounts of the purchases
 * already recorded change with it. The stock does not.
 */
export function useSaveEurXofRate() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: saveEurXofRate,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "eur-xof-rate"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizer-movements"] }),
            ]),
    });
}
