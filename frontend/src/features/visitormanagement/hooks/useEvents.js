import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelEvent,
    createEvent,
    fetchEvents,
    publishEvent,
    updateEvent,
} from "../api/visitormanagementApi";

const EVENTS_KEY = ["visitormanagement", "events"];
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