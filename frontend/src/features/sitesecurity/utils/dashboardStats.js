const PLACEHOLDER = "—";

/*
 * Every value comes from the API. While it loads, nothing is shown rather than a guess.
 * Labels are translation keys of the sitesecurity namespace, with their interpolation values.
 */
export function buildStatCards(stats, gateCount) {
  const value = (key) => (stats ? String(stats[key]) : PLACEHOLDER);
  const alerts = stats?.alerts ?? 0;
  return [
    { id: "points", labelKey: "dashboard.stats.points", value: value("activePoints"), hintKey: "dashboard.stats.pointsHint", hintValues: { count: gateCount } },
    {
      id: "zones",
      labelKey: "dashboard.stats.zones",
      value: value("controlledZones"),
      hintKey: stats ? "dashboard.stats.zonesHint" : null,
      hintValues: { count: stats?.totalZones ?? 0 },
    },
    { id: "visitors", labelKey: "dashboard.stats.visitors", value: value("visitors"), hintKey: "dashboard.stats.visitorsHint" },
    {
      id: "denied",
      labelKey: "dashboard.stats.denied",
      value: value("alerts"),
      hintKey: "dashboard.stats.deniedHint",
      alert: alerts > 0,
    },
  ];
}
