import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createHarvest } from "../api/plantsApi";

/**
 * Records a harvest.
 *
 * Only the harvest lists are refreshed afterwards, whatever their filter: the
 * API writes the harvest alone. The varieties and the yield forecast do not
 * depend on it.
 */
export function useRecordHarvest() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createHarvest,
        onSettled: () => queryClient.invalidateQueries({ queryKey: ["plants", "harvests"] }),
    });
}
