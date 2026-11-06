const PLACEHOLDER = "—";

/* Every value comes from the API. While it loads, nothing is shown rather than a guess. */
export function buildStatCards(stats, gateCount) {
    const value = (key) => (stats ? String(stats[key]) : PLACEHOLDER);
    const alerts = stats?.alerts ?? 0;
    return [
        { id: "points", label: "Active Access Points", value: value("activePoints"), sub: `Of ${gateCount} defined` },
        {
            id: "zones",
            label: "Controlled Zones",
            value: value("controlledZones"),
            sub: stats ? `Of ${stats.totalZones} zones defined` : PLACEHOLDER,
        },
        {
            id: "visitors",
            label: "Valid Visitor Credentials",
            value: value("visitors"),
            sub: "Passes, not a live on-site count",
        },
        {
            id: "denied",
            label: "Denied Access Events",
            value: value("alerts"),
            sub: "Recorded in the access log",
            alert: alerts > 0,
        },
    ];
}
