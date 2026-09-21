import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deliverBriefing, fetchBriefing, fetchStaff } from "../api/visitormanagementApi";

const BRIEFINGS_KEY = ["visitormanagement", "briefings"];
const STAFF_KEY = ["visitormanagement", "staff"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/**
 * The briefing of one registration. The API has no "list briefings" endpoint,
 * so the tracker fetches them one by one for the rows that carry a briefingId;
 * TanStack Query caches and deduplicates the calls.
 */
export function useBriefing(registrationId, enabled = true) {
    return useQuery({
        queryKey: [...BRIEFINGS_KEY, { registrationId }],
        queryFn: () => fetchBriefing(registrationId),
        enabled: Boolean(registrationId) && enabled,
    });
}

/**
 * The active staff members, offered as suggestions for the "delivered by"
 * field. Any role can deliver a briefing (front desk, guide, farm), so this is
 * not restricted to guides as the scheduling screen does.
 */
export function useActiveStaff() {
    return useQuery({
        queryKey: STAFF_KEY,
        queryFn: fetchStaff,
        select: (staff) => staff.filter((member) => member.active),
    });
}

/** Records a briefing as delivered, then refreshes the tracker and the KPI. */
export function useDeliverBriefing() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ registrationId, data }) => deliverBriefing(registrationId, data),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: BRIEFINGS_KEY });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}