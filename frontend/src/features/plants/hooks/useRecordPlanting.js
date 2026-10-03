import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createPlanting } from "../api/plantsApi";

/**
 * Records a planting.
 *
 * Every varieties and growth calendar query is refreshed afterwards, whatever
 * its filter: a planting can add a variety row, change a current tree count and
 * move the planting date of a block.
 */
export function useRecordPlanting() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createPlanting,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "varieties"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "growth-calendar"] }),
            ]),
    });
}
