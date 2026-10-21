import { useQuery } from "@tanstack/react-query";
import { fetchNurseryEvents } from "../api/plantsApi";

function nurseryEventsQueryKey({ batchId = null, eventType = null, from = null, to = null } = {}) {
    return ["plants", "nursery-events", { batchId, eventType, from, to }];
}

/**
 * Nursery events matching the given filters, in the order of the API: by
 * event date, then by record.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useNurseryEvents({ batchId = null, eventType = null, from = null, to = null } = {}) {
    return useQuery({
        queryKey: nurseryEventsQueryKey({ batchId, eventType, from, to }),
        queryFn: () => fetchNurseryEvents({ batchId, eventType, from, to }),
    });
}
