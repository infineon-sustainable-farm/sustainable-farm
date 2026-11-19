import { useMutation, useQueryClient } from "@tanstack/react-query";
import { saveGrowthPhaseShare } from "../api/plantsApi";

/**
 * Corrects the yield share of the growth phase `code`.
 *
 * The shares, every forecast and yield comparison, the harvest status, the
 * plant alerts, the overview and every scenario analysis are refreshed
 * afterwards: the API reads the shares on every one of them, so the expected
 * yields change with them, a share that becomes 0 or leaves 0 moves the first
 * month of a harvest window, and the years of a plan follow the shares.
 */
export function useSaveGrowthPhaseShare() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ code, share }) => saveGrowthPhaseShare(code, share),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "growth-phase-yield-shares"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-forecast"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-comparison"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "harvest-status"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "scenario-analysis"] }),
            ]),
    });
}
