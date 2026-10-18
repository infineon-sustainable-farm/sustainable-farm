import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
    createFertilizerApplication,
    createFertilizerLoss,
    createFertilizerPurchase,
} from "../api/plantsApi";

// One API route per movement type, keyed by the API's own code.
const RECORDERS = {
    PURCHASE: createFertilizerPurchase,
    APPLICATION: createFertilizerApplication,
    LOSS: createFertilizerLoss,
};

/**
 * Records a purchase, an application or a loss of a fertilizer, given as
 * `{ movementType, fertilizerId, body }`.
 *
 * The catalogue and every movement list are refreshed afterwards, whatever
 * their filter: a movement changes the stock of its fertilizer and adds a
 * line to the history. The API writes nothing else.
 */
export function useRecordFertilizerMovement() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ movementType, fertilizerId, body }) => RECORDERS[movementType](fertilizerId, body),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizers"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "fertilizer-movements"] }),
            ]),
    });
}
