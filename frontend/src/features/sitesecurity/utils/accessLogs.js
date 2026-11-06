export const RECENT_EVENT_COUNT = 4;

export function countLogs(logs) {
    const byStatus = (status) => logs.filter((e) => e.status === status).length;
    const byType = (type) => logs.filter((e) => e.type === type).length;
    return {
        all: logs.length,
        approved: byStatus("Approved"),
        denied: byStatus("Denied"),
        visitors: byType("Visitor"),
        staff: byType("Staff"),
        service: byType("Service"),
    };
}

export function filterByZone(logs, zone) {
    return zone ? logs.filter((e) => e.zone === zone) : logs;
}

/* The API returns logs newest first, so the most recent events are the first ones. */
export function recentEvents(logs) {
    return logs.slice(0, RECENT_EVENT_COUNT);
}
