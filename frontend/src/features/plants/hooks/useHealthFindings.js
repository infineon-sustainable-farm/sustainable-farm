import { useQuery } from "@tanstack/react-query";
import { fetchHealthFindings } from "../api/plantsApi";

function healthFindingsQueryKey({ blockCode = null, farmId = null, status = null, from = null, to = null } = {}) {
    return ["plants", "health-findings", { blockCode, farmId, status, from, to }];
}

/**
 * Findings matching the given filters, in the order of the API: by inspection
 * date. Their status and the date from which harvesting is allowed come
 * computed by the API.
 *
 * Loading, error and retry are handled by TanStack Query; the components read
 * its status flags instead of keeping their own.
 */
export function useHealthFindings({ blockCode = null, farmId = null, status = null, from = null, to = null } = {}) {
    return useQuery({
        queryKey: healthFindingsQueryKey({ blockCode, farmId, status, from, to }),
        queryFn: () => fetchHealthFindings({ blockCode, farmId, status, from, to }),
    });
}
