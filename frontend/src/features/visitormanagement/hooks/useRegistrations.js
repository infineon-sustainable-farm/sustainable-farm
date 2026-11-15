import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    approveRegistration,
    cancelRegistration,
    checkInRegistration,
    createRegistration,
    createVisitor,
    fetchAvailability,
    fetchProspects,
    fetchRegistrations,
    rejectRegistration,
} from "../api/visitormanagementApi";

const REGISTRATIONS_KEY = ["visitormanagement", "registrations"];
const VISITORS_KEY = ["visitormanagement", "visitors"];
const SLOTS_KEY = ["visitormanagement", "time-slots"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/** The registration queue, one page of up to 100 rows, newest first. */
export function useRegistrations() {
    const params = { page: 0, size: 100 };
    return useQuery({
        queryKey: [...REGISTRATIONS_KEY, params],
        queryFn: () => fetchRegistrations(params),
        select: (page) => page.content,
    });
}

/**
 * The bookable slots of a date; disabled until a date is chosen.
 */
export function useAvailability(date) {
    return useQuery({
        queryKey: [...SLOTS_KEY, "availability", { date }],
        queryFn: () => fetchAvailability(date),
        enabled: Boolean(date),
    });
}

/**
 * The commercial prospects, loaded only when the section is expanded so the
 * registration screen does not pay for an extra request by default.
 */
export function useProspects(enabled = true) {
    return useQuery({
        queryKey: [...REGISTRATIONS_KEY, "prospects"],
        queryFn: fetchProspects,
        enabled,
    });
}

/*
 * Registering is one or two API calls in order. When an existing visitor is
 * picked by name (the registration screen's main flow) only the registration
 * is sent; when the visitor is new the visitor is created first, then
 * registered on the slot. Chaining them in one mutation keeps the form's
 * pending and error state single. A failure on the second call leaves the
 * visitor created — a visitor without a registration is valid data, so no
 * rollback is attempted.
 */
export function useRegisterVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ visitorId, visitor, timeSlotId, visitPurpose }) => {
            const id = visitorId ?? (await createVisitor(visitor)).id;
            return createRegistration({
                visitorId: id,
                timeSlotId,
                visitPurpose,
            });
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: REGISTRATIONS_KEY });
            queryClient.invalidateQueries({ queryKey: VISITORS_KEY });
            queryClient.invalidateQueries({ queryKey: SLOTS_KEY });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}

/*
 * One mutation for the status transitions. `action` is "approve", "reject",
 * "check-in" or "cancel"; the backend enforces which transition is legal, and
 * the table only offers the buttons that match the row's current status.
 */
export function useRegistrationAction() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, action }) => {
            if (action === "approve") return approveRegistration(id);
            if (action === "reject") return rejectRegistration(id);
            if (action === "cancel") return cancelRegistration(id);
            return checkInRegistration(id);
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: REGISTRATIONS_KEY });
            queryClient.invalidateQueries({ queryKey: SLOTS_KEY });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}