import { apiClient } from "../../../shared/api/client";
import { VM_ENDPOINTS } from "./endpoints";

/**
 * Aggregated KPIs for the Visitor Management dashboard: weekly visitor counts,
 * booked slots, pending briefings, average satisfaction plus the upcoming
 * events and the pending staff tasks. No parameters.
 */
export function fetchDashboard() {
    return apiClient.get(VM_ENDPOINTS.DASHBOARD);
}

/**
 * Every time slot of the farm tour calendar. The backend offers a per-date
 * query as well, but the scheduling screen shows a whole week; one list call
 * is cheaper than six date calls, and TanStack Query caches it.
 */
export function fetchTimeSlots() {
    return apiClient.get(VM_ENDPOINTS.TIME_SLOTS);
}

/**
 * Creates a time slot. `data` is a TimeSlotRequest: date (YYYY-MM-DD),
 * startTime/endTime (HH:mm), maxCapacity (1–10) and an optional guideId.
 */
export function createTimeSlot(data) {
    return apiClient.post(VM_ENDPOINTS.TIME_SLOTS, data);
}

/** Updates a time slot with the same payload as creation. */
export function updateTimeSlot(id, data) {
    return apiClient.put(`${VM_ENDPOINTS.TIME_SLOTS}/${id}`, data);
}

/**
 * Cancels a time slot. The backend also cancels its registrations and
 * bookings, which is why callers must refetch both after this call.
 */
export function cancelTimeSlot(id) {
    return apiClient.delete(`${VM_ENDPOINTS.TIME_SLOTS}/${id}`);
}

/**
 * The registrations attached to one time slot, used to show who is booked in
 * each calendar cell. Returns a plain array.
 */
export function fetchRegistrationsBySlot(timeSlotId) {
    return apiClient.get(VM_ENDPOINTS.REGISTRATIONS, { params: { timeSlotId } });
}

/** Every staff member, used to fill the guide selectors. */
export function fetchStaff() {
    return apiClient.get(VM_ENDPOINTS.STAFF);
}