import { useMutation, useQueryClient } from "@tanstack/react-query";
import { saveGrowthPhaseShare } from "../api/plantsApi";

/**
 * Corrects the yield share of the growth phase `code`.
 *
 * The shares and every forecast are refreshed afterwards: the API reads the
 * shares on every forecast, so the expected yields change with them.
 */
export function useSaveGrowthPhaseShare() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ code, share }) => saveGrowthPhaseShare(code, share),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "growth-phase-yield-shares"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-forecast"] }),
            ]),
    });
}
