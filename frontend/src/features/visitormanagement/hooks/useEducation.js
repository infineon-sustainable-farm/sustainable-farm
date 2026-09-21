import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    createWorkshop,
    deactivateWorkshop,
    fetchTourStops,
    fetchWorkshops,
    publishWorkshop,
    updateWorkshop,
} from "../api/visitormanagementApi";

const TOUR_STOPS_KEY = ["visitormanagement", "education", "tour-stops"];
const WORKSHOPS_KEY = ["visitormanagement", "education", "workshops"];

/** The standard tour stops, already ordered by position by the API. */
export function useTourStops() {
    return useQuery({
        queryKey: TOUR_STOPS_KEY,
        queryFn: fetchTourStops,
    });
}

/** Every workshop, whatever its status, for the management table. */
export function useWorkshops() {
    return useQuery({
        queryKey: WORKSHOPS_KEY,
        queryFn: fetchWorkshops,
    });
}

function useInvalidateWorkshops() {
    const queryClient = useQueryClient();
    return () => queryClient.invalidateQueries({ queryKey: WORKSHOPS_KEY });
}

export function useCreateWorkshop() {
    const invalidate = useInvalidateWorkshops();
    return useMutation({
        mutationFn: createWorkshop,
        onSettled: invalidate,
    });
}

export function useUpdateWorkshop() {
    const invalidate = useInvalidateWorkshops();
    return useMutation({
        mutationFn: ({ id, data }) => updateWorkshop(id, data),
        onSettled: invalidate,
    });
}

/*
 * One mutation for publish and deactivate. The API only accepts publish on a
 * DRAFT and deactivate on an ACTIVE workshop, and the table only offers the
 * matching button, so a rejected call means the status changed elsewhere.
 */
export function useWorkshopStatusAction() {
    const invalidate = useInvalidateWorkshops();
    return useMutation({
        mutationFn: ({ id, action }) =>
            action === "publish" ? publishWorkshop(id) : deactivateWorkshop(id),
        onSettled: invalidate,
    });
}