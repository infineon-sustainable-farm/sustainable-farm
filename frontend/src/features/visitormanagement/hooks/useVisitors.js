import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    createVisitor,
    deleteVisitor,
    fetchVisitors,
    updateVisitor,
} from "../api/visitormanagementApi";

/*
 * Shared with the registration, safety and feedback screens: one key means a
 * visitor created or edited anywhere refreshes every visitor list.
 */
const VISITORS_KEY = ["visitormanagement", "visitors"];

/** Every visitor, used by the directory and the visitor selectors. */
export function useVisitors() {
    return useQuery({
        queryKey: VISITORS_KEY,
        queryFn: fetchVisitors,
    });
}

export function useCreateVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createVisitor,
        onSettled: () =>
            queryClient.invalidateQueries({ queryKey: VISITORS_KEY }),
    });
}

export function useUpdateVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, visitor }) => updateVisitor(id, visitor),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: VISITORS_KEY });
            queryClient.invalidateQueries({ queryKey: ["visitormanagement", "registrations"] });
        },
    });
}

export function useDeleteVisitor() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteVisitor,
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: VISITORS_KEY });
            queryClient.invalidateQueries({ queryKey: ["visitormanagement", "registrations"] });
        },
    });
}