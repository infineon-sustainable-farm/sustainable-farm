import { useEffect, useMemo, useState } from "react";
import { getAllAlerts } from "../api/energyApi";

const severityBadge = (s) => {
  const map = {
    Critical: "bg-destructive/10 text-destructive",
    Warning: "bg-warning/10 text-warning",
    Info: "bg-primary/10 text-primary",
  };
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[s] || map.Info}`}>{s}</span>;
};

const statusPill = (status) => {
  const cls = status === "Open" ? "bg-destructive/10 text-destructive" : "bg-secondary/10 text-secondary";
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${cls}`}>{status}</span>;
};

export default function AlertsNotifications() {
  const [alerts, setAlerts] = useState(null);
  const [filter, setFilter] = useState("All");
  const [error, setError] = useState(null);

  useEffect(() => {
    getAllAlerts()
      .then(setAlerts)
      .catch((err) => setError(err.message));
  }, []);

  const rows = useMemo(
    () => (!alerts ? [] : filter === "All" ? alerts : alerts.filter((a) => a.status === filter)),
    [alerts, filter]
  );

  if (error) return <p className="text-sm text-destructive">Failed to load alerts: {error}</p>;
  if (!alerts) return <p className="text-sm text-muted-foreground">Loading alerts...</p>;

  const openCount = alerts.filter((a) => a.status === "Open").length;
  const criticalCount = alerts.filter((a) => a.severity === "Critical").length;

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">Alerts &amp; Notifications</h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          {openCount} open · {criticalCount} critical requiring attention across the energy system.
        </p>
      </div>

      <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary">
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">System Alerts</h2>
          <div className="flex items-center gap-1 rounded-md bg-muted p-0.5">
            {["All", "Open", "Resolved"].map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`rounded px-3 py-1 text-xs font-semibold transition-colors ${
                  filter === f ? "bg-card text-primary shadow-sm" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {f}
              </button>
            ))}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr>
                {["Alert ID", "Severity", "Component", "Message", "Timestamp", "Status"].map((h) => (
                  <th key={h} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr><td colSpan={6} className="px-5 py-8 text-center text-muted-foreground">No alerts match this filter.</td></tr>
              )}
              {rows.map((a) => (
                <tr key={a.alertId} className="hover:bg-muted/40">
                  <td className="border-b border-border px-5 py-3 font-semibold">{a.alertId}</td>
                  <td className="border-b border-border px-5 py-3">{severityBadge(a.severity)}</td>
                  <td className="border-b border-border px-5 py-3 whitespace-nowrap font-medium">{a.componentId}</td>
                  <td className="border-b border-border px-5 py-3 min-w-64 text-muted-foreground">{a.message}</td>
                  <td className="border-b border-border px-5 py-3 whitespace-nowrap text-muted-foreground tabular-nums">{a.timestamp}</td>
                  <td className="border-b border-border px-5 py-3">{statusPill(a.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}