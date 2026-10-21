import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFindingTreatment, createPreventiveTreatment } from "../api/plantsApi";

/**
 * Records a treatment answering the finding `findingId`, or a preventive
 * treatment when `findingId` is null.
 *
 * The treatment record, every findings list and the plant alerts are refreshed
 * afterwards, whatever their filter: a treatment changes the status and the
 * harvest date of its finding, and can start a pre-harvest interval. The
 * catalogue is refreshed too, as the API adds "Other" the first time a
 * preventive treatment uses it.
 */
export function useRecordTreatment() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ findingId, treatment }) =>
            findingId === null ? createPreventiveTreatment(treatment) : createFindingTreatment(findingId, treatment),
        onSettled: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["plants", "treatments"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "health-findings"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "alerts"] }),
                queryClient.invalidateQueries({ queryKey: ["plants", "health-issues"] }),
            ]),
    });
}
