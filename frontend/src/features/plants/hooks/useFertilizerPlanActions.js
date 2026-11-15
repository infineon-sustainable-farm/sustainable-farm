import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelFertilizerPlan, completeFertilizerPlan, createFertilizerPlan } from "../api/plantsApi";

/** Every list, comparison and stock check of the planned applications, whatever their filter. */
const PLANS_KEY = ["plants", "fertilizer-plans"];

/**
 * Plans an application of a fertilizer, given as the request body.
 *
 * The plans, their comparison and their stock check are refreshed afterwards:
 * a new plan adds a line, a planned quantity and a need of stock. The stock
 * itself does not move.
 */
export function usePlanFertilizerApplication() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (body) => createFertilizerPlan(body),
        onSettled: () => queryClient.invalidateQueries({ queryKey: PLANS_KEY }),
    });
}

/**
 * Marks a planned application as done, given as `{ planId, body }`.
 *
 * The API records the real application, so everything a movement refreshes
 * is refreshed too: the catalogue, the movements, the plant alerts and the
 * overview. All of it is refreshed even when the API refuses: a 409 means
 * the plan was done or cancelled elsewhere, and its row must show it.
 */
export function useCompleteFertilizerPlan() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ planId, body }) => completeFertilizerPlan(planId, body),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: PLANS_KEY }),
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizers"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizer-movements"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "overview"] }),
            ]),
    });
}

/**
 * Cancels a planned application, given as its identifier. The plans are
 * refreshed afterwards, also after a 409, for the same reason as above.
 */
export function useCancelFertilizerPlan() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (planId) => cancelFertilizerPlan(planId),
        onSettled: () => queryClient.invalidateQueries({ queryKey: PLANS_KEY }),
    });
}
