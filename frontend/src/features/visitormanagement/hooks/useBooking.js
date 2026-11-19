import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelBooking,
    completeBooking,
    confirmBooking,
    createActivity,
    createBooking,
    deactivateActivity,
    fetchActivities,
    fetchBookings,
    fetchOccupancy,
    payBooking,
    updateActivity,
    updateBooking,
} from "../api/visitormanagementApi";

const BOOKINGS_KEY = ["visitormanagement", "bookings"];
const ACTIVITIES_KEY = ["visitormanagement", "activities"];
const OCCUPANCY_KEY = ["visitormanagement", "occupancy"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/**
 * The booking queue. Without filters the API returns a Page (newest first);
 * with a filter it returns a plain array, so the select normalises both to a
 * list. Filters are exclusive on the backend and the screen keeps only one
 * active at a time.
 */
export function useBookings(filters = {}) {
    const { status = "", activityId = "", date = "" } = filters;
    const hasFilter = Boolean(status || activityId || date);
    const query = hasFilter ? { status, activityId, date } : { page: 0, size: 100 };
    return useQuery({
        queryKey: [...BOOKINGS_KEY, query],
        queryFn: () => fetchBookings(query),
        select: (data) => (Array.isArray(data) ? data : data.content),
    });
}

/** Every activity, used by the booking form and the occupancy section. */
export function useActivities() {
    return useQuery({
        queryKey: ACTIVITIES_KEY,
        queryFn: fetchActivities,
    });
}

/** Occupancy per activity, used by the progress bars. */
export function useOccupancy() {
    return useQuery({
        queryKey: OCCUPANCY_KEY,
        queryFn: fetchOccupancy,
    });
}

/*
 * Bookings, occupancy and the dashboard all move together: a booking change
 * alters the occupancy bars and the dashboard KPIs. Activities are only
 * invalidated when an activity is created, since that adds a bar.
 */
function useInvalidateBookings() {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: BOOKINGS_KEY });
        queryClient.invalidateQueries({ queryKey: OCCUPANCY_KEY });
        queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
    };
}

export function useCreateActivity() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createActivity,
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: ACTIVITIES_KEY });
            queryClient.invalidateQueries({ queryKey: OCCUPANCY_KEY });
        },
    });
}

export function useUpdateActivity() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, data }) => updateActivity(id, data),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: ACTIVITIES_KEY });
            queryClient.invalidateQueries({ queryKey: OCCUPANCY_KEY });
        },
    });
}

/*
 * Deactivating an activity also cancels its bookings on the backend, so the
 * bookings list, the occupancy and the dashboard are all refreshed.
 */
export function useDeactivateActivity() {
    const invalidate = useInvalidateBookings();
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deactivateActivity,
        onSettled: () => {
            invalidate();
            queryClient.invalidateQueries({ queryKey: ACTIVITIES_KEY });
        },
    });
}

export function useCreateBooking() {
    const invalidate = useInvalidateBookings();
    return useMutation({
        mutationFn: createBooking,
        onSettled: invalidate,
    });
}

export function useUpdateBooking() {
    const invalidate = useInvalidateBookings();
    return useMutation({
        mutationFn: ({ id, data }) => updateBooking(id, data),
        onSettled: invalidate,
    });
}

/*
 * One mutation for pay, confirm, complete and cancel. The API rejects a
 * transition the current status does not allow, so a failure usually means the
 * booking moved on another screen; the message is shown as-is.
 */
export function useBookingAction() {
    const invalidate = useInvalidateBookings();
    return useMutation({
        mutationFn: ({ id, action }) => {
            if (action === "pay") return payBooking(id);
            if (action === "confirm") return confirmBooking(id);
            if (action === "complete") return completeBooking(id);
            return cancelBooking(id);
        },
        onSettled: invalidate,
    });
}