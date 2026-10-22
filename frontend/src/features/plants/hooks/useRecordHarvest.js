import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createHarvest } from "../api/plantsApi";

/**
 * Records a harvest.
 *
 * The harvest lists, the yield comparison and the harvest status are
 * refreshed afterwards, whatever their filter: the API writes the harvest
 * alone, but the comparison adds it to the actual yield, and the status shows
 * the first harvest of an open harvest window. The varieties, the yield
 * forecast and the alerts do not depend on it.
 */
export function useRecordHarvest() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createHarvest,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "harvests"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-comparison"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "harvest-status"] }),
            ]),
    });
}
