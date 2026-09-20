import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";

/**
 * Aggregated KPIs for the Visitor Management dashboard: weekly visitor counts,
 * booked slots, pending briefings, average satisfaction plus the upcoming
 * events and the pending staff tasks. No parameters.
 */
export function fetchDashboard() {
    return apiClient.get(ENDPOINTS.VM_DASHBOARD);
}