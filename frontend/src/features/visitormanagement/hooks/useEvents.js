import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelEvent,
    createEvent,
    fetchEventRegistrations,
    fetchEvents,
    publishEvent,
    registerEventAttendee,
    updateEvent,
} from "../api/visitormanagementApi";

const EVENTS_KEY = ["visitormanagement", "events"];
const EVENT_REGISTRATIONS_KEY = ["visitormanagement", "events", "registrations"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/** The events, one page of up to 100 rows, most recent start first. */
export function useEvents() {
    const params = { page: 0, size: 100 };
    return useQuery({
        queryKey: [...EVENTS_KEY, params],
        queryFn: () => fetchEvents(params),
        select: (page) => page.content,
    });
}

/*
 * Every event change also moves the dashboard's upcoming events list, so all
 * mutations invalidate both keys.
 */
function useInvalidateEvents() {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: EVENTS_KEY });
        queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
    };
}

export function useCreateEvent() {
    const invalidate = useInvalidateEvents();
    return useMutation({
        mutationFn: createEvent,
        onSettled: invalidate,
    });
}

export function useUpdateEvent() {
    const invalidate = useInvalidateEvents();
    return useMutation({
        mutationFn: ({ id, data }) => updateEvent(id, data),
        onSettled: invalidate,
    });
}

/*
 * One mutation for publish and cancel. The API only accepts publish on a DRAFT
 * event, and cancel on anything not COMPLETED, so the cards only offer the
 * matching button.
 */
export function useEventStatusAction() {
    const invalidate = useInvalidateEvents();
    return useMutation({
        mutationFn: ({ id, action }) =>
            action === "publish" ? publishEvent(id) : cancelEvent(id),
        onSettled: invalidate,
    });
}

/**
 * The participants of one event. Disabled until an event is selected in the
 * panel.
 */
export function useEventRegistrations(eventId, enabled = true) {
    return useQuery({
        queryKey: [...EVENT_REGISTRATIONS_KEY, { eventId }],
        queryFn: () => fetchEventRegistrations(eventId),
        enabled: Boolean(eventId) && enabled,
    });
}

/*
 * Registering an attendee changes the participant list, the event's booked
 * count and the dashboard, so all three keys are invalidated.
 */
export function useRegisterEventAttendee() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ eventId, data }) => registerEventAttendee(eventId, data),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: EVENT_REGISTRATIONS_KEY });
            queryClient.invalidateQueries({ queryKey: EVENTS_KEY });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}