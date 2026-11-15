/*
 * The two groups the alerts are shown in. The group is a display priority
 * read from what the API sends for each alert, its type and its severity; no
 * threshold or date is compared here.
 * - Act now: every CRITICAL alert, every open health problem and every low
 *   stock.
 * - Plan ahead: the others, that is a pre-harvest interval outside the
 *   harvest season, a batch ready to transplant and a harvest season
 *   approaching. A type the API adds later is planned ahead unless critical.
 */
export const ALERT_PRIORITIES = [
    { priority: "ACT_NOW", label: "Act now" },
    { priority: "PLAN_AHEAD", label: "Plan ahead" },
];

const ACT_NOW_TYPES = new Set(["OPEN_HEALTH_ISSUE", "LOW_STOCK"]);

/** The group of an alert: "ACT_NOW" or "PLAN_AHEAD". */
export function alertPriority(alert) {
    return alert.severity === "CRITICAL" || ACT_NOW_TYPES.has(alert.type) ? "ACT_NOW" : "PLAN_AHEAD";
}

/**
 * The alerts in their groups, Act now first. Inside a group the alerts keep
 * the order they came in, the most urgent first; a group without alert is
 * left out.
 */
export function groupAlertsByPriority(alerts) {
    return ALERT_PRIORITIES.map((group) => ({
        ...group,
        alerts: (alerts ?? []).filter((alert) => alertPriority(alert) === group.priority),
    })).filter((group) => group.alerts.length > 0);
}

/** How many of the alerts are to act on now. */
export function countActNow(alerts) {
    return (alerts ?? []).filter((alert) => alertPriority(alert) === "ACT_NOW").length;
}
