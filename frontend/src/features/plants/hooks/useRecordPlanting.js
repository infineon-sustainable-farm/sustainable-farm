import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createPlanting } from "../api/plantsApi";

/**
 * Records a planting.
 *
 * Every varieties, growth calendar and yield forecast query is refreshed
 * afterwards, whatever its filter: a planting can add a variety row, change a
 * current tree count, move the planting date of a block and add trees to the
 * forecast.
 */
export function useRecordPlanting() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createPlanting,
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "varieties"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "growth-calendar"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "yield-forecast"] }),
            ]),
    });
}
