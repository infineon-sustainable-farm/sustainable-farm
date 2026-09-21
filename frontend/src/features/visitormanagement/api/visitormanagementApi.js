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

/** Every visitor, used to join language and type onto registrations. */
export function fetchVisitors() {
    return apiClient.get(VM_ENDPOINTS.VISITORS);
}

/** Creates a visitor from a VisitorRequest payload. */
export function createVisitor(data) {
    return apiClient.post(VM_ENDPOINTS.VISITORS, data);
}

/** Updates a visitor from a VisitorRequest payload. */
export function updateVisitor(id, data) {
    return apiClient.put(`${VM_ENDPOINTS.VISITORS}/${id}`, data);
}

/**
 * The registration queue, most recent first. The endpoint is paginated; the
 * screen asks for one large page rather than hiding rows behind pagination.
 */
export function fetchRegistrations({ page = 0, size = 100 } = {}) {
    return apiClient.get(VM_ENDPOINTS.REGISTRATIONS, { params: { page, size } });
}

/**
 * Registers a visitor on a time slot. `data` is a RegistrationRequest:
 * visitorId, timeSlotId, visitPurpose and an optional eventId.
 */
export function createRegistration(data) {
    return apiClient.post(VM_ENDPOINTS.REGISTRATIONS, data);
}

/*
 * Status transitions are PATCH calls without a body, as the API defines them.
 * The backend rejects a transition that the current status does not allow, and
 * the screen surfaces that message as-is.
 */
export function approveRegistration(id) {
    return apiClient.patch(`${VM_ENDPOINTS.REGISTRATIONS}/${id}/approve`);
}

export function rejectRegistration(id) {
    return apiClient.patch(`${VM_ENDPOINTS.REGISTRATIONS}/${id}/reject`);
}

export function checkInRegistration(id) {
    return apiClient.patch(`${VM_ENDPOINTS.REGISTRATIONS}/${id}/check-in`);
}

/**
 * The bookable slots of a date with their remaining capacity, used by the
 * registration form. Cancelled slots are already filtered out by the API.
 */
export function fetchAvailability(date) {
    return apiClient.get(`${VM_ENDPOINTS.TIME_SLOTS}/availability`, { params: { date } });
}