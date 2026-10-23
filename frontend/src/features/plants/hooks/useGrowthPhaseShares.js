import { useQuery } from "@tanstack/react-query";
import { fetchGrowthPhaseShares } from "../api/plantsApi";

/**
 * The yield share of every growth phase, the youngest first. Every phase has
 * one: its default until the user corrects it.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useGrowthPhaseShares() {
    return useQuery({
        queryKey: ["plants", "growth-phase-yield-shares"],
        queryFn: fetchGrowthPhaseShares,
    });
}
