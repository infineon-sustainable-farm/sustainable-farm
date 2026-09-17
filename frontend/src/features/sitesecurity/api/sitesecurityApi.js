import { apiClient } from "../../../shared/api/client";
import { ENDPOINTS } from "../../../shared/api/endpoints";
import { GATES, LOGS, USERS, ZONES } from "./sitesecurityMockFixtures";

// Mock data is only ever used as a development fallback, never in production.
const DEV = import.meta.env.DEV;

function devFallback(warn, data) {
    if (DEV) {
        console.warn(`[sitesecurity] ${warn} — serving mock fixtures (development only).`);
        return Promise.resolve(data);
    }
    throw new Error(warn);
}

export function fetchSecurityOverview() {
    return apiClient
        .get(`${ENDPOINTS.SITESECURITY}/overview`)
        .catch((err) =>
            devFallback(`Failed to load security overview: ${err.message}`, {
                gates: GATES,
                stats: { activePoints: 4, controlledZones: 3, visitors: 2, alerts: 1 },
            })
        );
}

export function fetchZones() {
    return apiClient
        .get(`${ENDPOINTS.SITESECURITY}/zones`)
        .catch((err) => devFallback(`Failed to load zones: ${err.message}`, ZONES));
}

export function fetchCredentials() {
    return apiClient
        .get(`${ENDPOINTS.SITESECURITY}/credentials`)
        .catch((err) => devFallback(`Failed to load credentials: ${err.message}`, USERS));
}

export function createCredentialApi(credential) {
    return apiClient
        .post(`${ENDPOINTS.SITESECURITY}/credentials`, credential)
        .catch((err) => {
            throw new Error(err.message || "Failed to create credential. Please try again.");
        });
}

export function fetchAccessLogs(filter) {
    return apiClient
        .get(`${ENDPOINTS.SITESECURITY}/logs`, { params: { filter } })
        .catch((err) =>
            devFallback(
                `Failed to load access logs: ${err.message}`,
                filter === "all" ? LOGS : LOGS.filter(matchesFilter(filter))
            )
        );
}

function matchesFilter(f) {
    return (e) => {
        switch (f) {
            case "all":
                return true;
            case "approved":
                return e.status === "Approved";
            case "denied":
                return e.status === "Denied";
            case "visitors":
                return e.type === "Visitor";
            case "staff":
                return e.type === "Staff";
            case "service":
                return e.type === "Service";
            default:
                return true;
        }
    };
}
