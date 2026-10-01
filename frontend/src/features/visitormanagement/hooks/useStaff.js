import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    createStaff,
    deactivateStaff,
    fetchStaff,
    updateStaff,
} from "../api/visitormanagementApi";

/*
 * Shared with the scheduling guide selectors and the safety "delivered by"
 * suggestions: one key means a staff change refreshes every screen that lists
 * staff.
 */
const STAFF_KEY = ["visitormanagement", "staff"];

/** Every staff member, ordered by name by the API. */
export function useStaffList() {
    return useQuery({
        queryKey: STAFF_KEY,
        queryFn: fetchStaff,
    });
}

function useInvalidateStaff() {
    const queryClient = useQueryClient();
    return () => queryClient.invalidateQueries({ queryKey: STAFF_KEY });
}

export function useCreateStaff() {
    const invalidate = useInvalidateStaff();
    return useMutation({
        mutationFn: createStaff,
        onSettled: invalidate,
    });
}

export function useUpdateStaff() {
    const invalidate = useInvalidateStaff();
    return useMutation({
        mutationFn: ({ id, data }) => updateStaff(id, data),
        onSettled: invalidate,
    });
}

/**
 * Deactivates a member (DELETE) or brings one back (PUT with active=true, the
 * only reactivation path the API offers). The row carries every field the
 * update payload needs.
 */
export function useStaffStatusAction() {
    const invalidate = useInvalidateStaff();
    return useMutation({
        mutationFn: ({ staff, action }) =>
            action === "activate"
                ? updateStaff(staff.id, {
                      fullName: staff.fullName,
                      role: staff.role,
                      email: staff.email,
                      phone: staff.phone,
                      active: true,
                  })
                : deactivateStaff(staff.id),
        onSettled: invalidate,
    });
}