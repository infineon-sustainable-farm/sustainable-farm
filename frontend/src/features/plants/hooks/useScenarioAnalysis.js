import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchScenarioAnalysis } from "../api/plantsApi";

/**
 * The analysis of the scenario `scenarioId` for a variant and a horizon, as
 * the API computes it; `params` holds only the parameters given (see
 * analysisParams), so each variant has its own cached analysis.
 *
 * The data is `{ params, analysis }`: the analysis comes with the parameters it
 * was computed for. With `keepPrevious`, the analysis of the previous
 * parameters of the same scenario stays shown while the next one loads, and
 * `params` says which variant it is, so a label never names a variant the
 * figures are not for. Another scenario's analysis is never kept.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useScenarioAnalysis(scenarioId, params, { enabled = true, keepPrevious = false } = {}) {
    return useQuery({
        queryKey: ["plants", "scenario-analysis", scenarioId, params],
        queryFn: () => fetchScenarioAnalysis(scenarioId, params).then((analysis) => ({ params, analysis })),
        enabled: enabled && scenarioId !== null,
        placeholderData: keepPrevious
            ? (previous, previousQuery) => (previousQuery?.queryKey[2] === scenarioId ? previous : undefined)
            : undefined,
    });
}

/**
 * `value` once it has stopped changing for `delayMs`: a slider dragged, or a
 * number typed digit by digit, asks the API once for the value it settles on.
 * Meant for text and numbers, compared by value.
 */
export function useDebouncedValue(value, delayMs = 300) {
    const [settled, setSettled] = useState(value);

    useEffect(() => {
        const timer = setTimeout(() => setSettled(value), delayMs);
        return () => clearTimeout(timer);
    }, [value, delayMs]);

    return settled;
}
