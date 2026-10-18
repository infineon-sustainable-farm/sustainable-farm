import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelTimeSlot,
    createTimeSlot,
    fetchRegistrationsBySlot,
    fetchStaff,
    fetchTimeSlots,
    updateTimeSlot,
} from "../api/visitormanagementApi";

/*
 * Cache keys of the module. Mutations invalidate by prefix, so every screen
 * reading slots, registrations or the dashboard refreshes after a change.
 */
const SLOTS_KEY = ["visitormanagement", "time-slots"];
const REGISTRATIONS_KEY = ["visitormanagement", "registrations"];
const STAFF_KEY = ["visitormanagement", "staff"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/** Every slot of the tour calendar, for the week currently on screen. */
export function useTimeSlots() {
    return useQuery({
        queryKey: SLOTS_KEY,
        queryFn: fetchTimeSlots,
    });
}

/**
 * The active guides, for the guide filter and the slot form. Staff is fetched
 * whole (the API has no role filter) and reduced to active guides client-side,
 * so other staff roles never show up in a guide selector.
 */
export function useGuides() {
    return useQuery({
        queryKey: STAFF_KEY,
        queryFn: fetchStaff,
        select: (staff) => staff.filter((member) => member.role === "GUIDE" && member.active),
    });
}

/**
 * Registrations of one slot. Disabled when there is no slot id, or when the
 * caller passes `enabled: false` (cells without bookings), so the query never
 * fires for empty calendar cells; TanStack Query deduplicates the calls shared
 * between cells.
 */
export function useSlotRegistrations(timeSlotId, enabled = true) {
    return useQuery({
        queryKey: [...REGISTRATIONS_KEY, { timeSlotId }],
        queryFn: () => fetchRegistrationsBySlot(timeSlotId),
        enabled: Boolean(timeSlotId) && enabled,
    });
}

/*
 * Cancelling a slot cascades into its registrations and bookings on the
 * backend, so every mutation invalidates all three lists. `onSettled` runs on
 * failure too: a rejected request can still have changed nothing, but refetch
 * is the safe default and keeps the grid honest.
 */
function useInvalidateScheduling() {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: SLOTS_KEY });
        queryClient.invalidateQueries({ queryKey: REGISTRATIONS_KEY });
        queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
    };
}

export function useCreateTimeSlot() {
    const invalidate = useInvalidateScheduling();
    return useMutation({
        mutationFn: createTimeSlot,
        onSettled: invalidate,
    });
}

export function useUpdateTimeSlot() {
    const invalidate = useInvalidateScheduling();
    return useMutation({
        mutationFn: ({ id, data }) => updateTimeSlot(id, data),
        onSettled: invalidate,
    });
}

export function useCancelTimeSlot() {
    const invalidate = useInvalidateScheduling();
    return useMutation({
        mutationFn: cancelTimeSlot,
        onSettled: invalidate,
    });
}