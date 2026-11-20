import { useQuery } from "@tanstack/react-query";
import {
    fetchFertilizerPlanComparison,
    fetchFertilizerPlans,
    fetchFertilizerPlanStockCheck,
} from "../api/plantsApi";

/**
 * Planned applications of fertilizer matching the given status, in the order
 * of the API: by planned date, then by record. Each one comes with its delay
 * and its cost at the last purchase price, as the API computes them.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useFertilizerPlans({ status = null } = {}) {
    return useQuery({
        queryKey: ["plants", "fertilizer-plans", "list", { status }],
        queryFn: () => fetchFertilizerPlans({ status }),
    });
}

/**
 * Planned and applied quantities by block and fertilizer over a period. With
 * no date, the API takes the current calendar year and returns the period.
 */
export function useFertilizerPlanComparison({ from = null, to = null } = {}) {
    return useQuery({
        queryKey: ["plants", "fertilizer-plans", "comparison", { from, to }],
        queryFn: () => fetchFertilizerPlanComparison({ from, to }),
    });
}

/**
 * The fertilizers whose stock does not cover the applications planned in the
 * next 30 days, with the day the API computed it for. That day is also the
 * one the plan forms compare their dates with.
 */
export function useFertilizerPlanStockCheck() {
    return useQuery({
        queryKey: ["plants", "fertilizer-plans", "stock-check"],
        queryFn: fetchFertilizerPlanStockCheck,
    });
}
