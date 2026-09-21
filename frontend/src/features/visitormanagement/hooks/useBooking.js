import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelBooking,
    completeBooking,
    confirmBooking,
    createActivity,
    createBooking,
    fetchActivities,
    fetchBookings,
    fetchOccupancy,
    payBooking,
    updateBooking,
} from "../api/visitormanagementApi";

const BOOKINGS_KEY = ["visitormanagement", "bookings"];
const ACTIVITIES_KEY = ["visitormanagement", "activities"];
const OCCUPANCY_KEY = ["visitormanagement", "occupancy"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/** The booking queue, one page of up to 100 rows, newest first. */
export function useBookings() {
    const params = { page: 0, size: 100 };
    return useQuery({
        queryKey: [...BOOKINGS_KEY, params],
        queryFn: () => fetchBookings(params),
        select: (page) => page.content,
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