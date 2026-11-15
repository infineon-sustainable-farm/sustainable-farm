import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createScenario, updateScenario } from "../api/plantsApi";

/**
 * Enters a scenario when `id` is null, or corrects the scenario `id`.
 *
 * The scenarios and every analysis are refreshed afterwards: the API computes
 * each analysis from the assumptions on every read, so all its figures change
 * with them.
 */
export function useSaveScenario() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, scenario }) => (id === null ? createScenario(scenario) : updateScenario(id, scenario)),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "scenarios"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "scenario-analysis"] }),
            ]),
    });
}
