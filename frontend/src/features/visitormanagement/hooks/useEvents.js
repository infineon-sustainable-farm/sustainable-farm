import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelEvent,
    createEvent,
    createVisitor,
    fetchEventRegistrations,
    fetchEvents,
    publishEvent,
    registerEventAttendee,
    updateEvent,
} from "../api/visitormanagementApi";

const EVENTS_KEY = ["visitormanagement", "events"];
const EVENT_REGISTRATIONS_KEY = ["visitormanagement", "events", "registrations"];
const DASHBOARD_KEY = ["visitormanagement", "dashboard"];

/**
 * The events. Without filters the API returns a Page (most recent start
 * first); with a filter it returns a plain array, so the select normalises
 * both to a list. Filters are exclusive on the backend.
 */
export function useEvents(filters = {}) {
    const { type = "", date = "" } = filters;
    const hasFilter = Boolean(type || date);
    const query = hasFilter ? { type, date } : { page: 0, size: 100 };
    return useQuery({
        queryKey: [...EVENTS_KEY, query],
        queryFn: () => fetchEvents(query),
        select: (data) => (Array.isArray(data) ? data : data.content),
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

/**
 * The registration screen's event target: when an existing visitor is picked
 * by name, only the attendee registration is sent with that visitor id; for a
 * new visitor, the visitor is created first, then registered on the event.
 * Chaining both calls in one mutation keeps a single pending/error state; a
 * failure on the second call leaves the visitor created, which is valid data.
 */
export function useRegisterEventVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ visitorId, visitor, eventId, visitPurpose }) => {
            const id = visitorId ?? (await createVisitor(visitor)).id;
            return registerEventAttendee(eventId, {
                visitorId: id,
                visitPurpose,
            });
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: EVENT_REGISTRATIONS_KEY });
            queryClient.invalidateQueries({ queryKey: EVENTS_KEY });
            queryClient.invalidateQueries({ queryKey: ["visitormanagement", "registrations"] });
            queryClient.invalidateQueries({ queryKey: ["visitormanagement", "visitors"] });
            queryClient.invalidateQueries({ queryKey: DASHBOARD_KEY });
        },
    });
}