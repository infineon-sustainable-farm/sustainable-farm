import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
    createNurseryLoss,
    createNurseryStageChange,
    createNurseryTransplant,
} from "../api/plantsApi";
import { PLANTING_QUERY_KEYS } from "./useRecordPlanting";

// One API route per event type, keyed by the API's own code.
const RECORDERS = {
    STAGE_CHANGE: createNurseryStageChange,
    LOSS: createNurseryLoss,
    TRANSPLANT: createNurseryTransplant,
};

// What every event changes: its batch, the history, the batch's alert, and so the overview.
const NURSERY_QUERY_KEYS = [
    ["plants", "nursery-batches"],
    ["plants", "nursery-events"],
    ["plants", "alerts"],
    ["plants", "overview"],
];

// A transplant is also a planting: its batch, the history, and what a planting changes, alerts included.
const TRANSPLANT_QUERY_KEYS = [["plants", "nursery-batches"], ["plants", "nursery-events"], ...PLANTING_QUERY_KEYS];

/**
 * Records a stage change, a loss or a transplant of a nursery batch, given as
 * `{ eventType, batchId, body }`.
 *
 * The batches, every history list, the plant alerts and the overview are
 * refreshed afterwards, whatever their filter: an event changes the stage or
 * the plants left of its batch, and so its alert. A transplant is also a
 * planting, as the API records the batch's variety on the block in the same
 * transaction: every query a planting refreshes is refreshed too.
 */
export function useRecordNurseryEvent() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ eventType, batchId, body }) => RECORDERS[eventType](batchId, body),
        onSettled: (data, error, { eventType }) => {
            const queryKeys = eventType === "TRANSPLANT" ? TRANSPLANT_QUERY_KEYS : NURSERY_QUERY_KEYS;
            return Promise.all(queryKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })));
        },
    });
}
