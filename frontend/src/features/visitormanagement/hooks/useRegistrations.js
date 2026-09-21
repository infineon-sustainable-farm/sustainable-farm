import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    approveRegistration,
    checkInRegistration,
    createRegistration,
    createVisitor,
    fetchAvailability,
    fetchRegistrations,
    fetchVisitors,
    rejectRegistration,
    updateVisitor,
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
 * Every visitor, used to show language and type on the queue (the registration
 * payload only carries the visitor id) and to prefill the edit form.
 */
export function useVisitors() {
    return useQuery({
        queryKey: VISITORS_KEY,
        queryFn: fetchVisitors,
    });
}

/** The bookable slots of a date; disabled until a date is chosen. */
export function useAvailability(date) {
    return useQuery({
        queryKey: [...SLOTS_KEY, "availability", { date }],
        queryFn: () => fetchAvailability(date),
        enabled: Boolean(date),
    });
}

/*
 * Registering is two API calls in order: the visitor is created first, then
 * registered on the slot. Chaining them in one mutation keeps the form's
 * pending and error state single. A failure on the second call leaves the
 * visitor created — a visitor without a registration is valid data, so no
 * rollback is attempted.
 */
export function useRegisterVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ visitor, timeSlotId, visitPurpose }) => {
            const created = await createVisitor(visitor);
            return createRegistration({
                visitorId: created.id,
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

/** Saves the visitor fields edited from a queue row. */
export function useUpdateVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, visitor }) => updateVisitor(id, visitor),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: VISITORS_KEY });
            queryClient.invalidateQueries({ queryKey: REGISTRATIONS_KEY });
        },
    });
}

/*
 * One mutation for the three status transitions. `action` is "approve",
 * "reject" or "check-in"; the backend enforces which transition is legal, and
 * the table only offers the buttons that match the row's current status.
 */
export function useRegistrationAction() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, action }) => {
            if (action === "approve") return approveRegistration(id);
            if (action === "reject") return rejectRegistration(id);
            return checkInRegistration(id);
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: REGISTRATIONS_KEY });
            queryClient.invalidateQueries({ queryKey: SLOTS_KEY });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}