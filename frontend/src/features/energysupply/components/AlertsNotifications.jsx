import { useCallback, useEffect, useMemo, useState } from "react";
import { getAllAlerts, updateAlertStatus } from "../api/energyApi";
import { formatEnergyLabel } from "../utils/formatEnergyLabel";

const isStatus = (alert, status) => alert.status === status.toUpperCase();

const severityBadge = (s) => {
  const map = {
    CRITICAL: "bg-error/10 text-error",
    WARNING: "bg-warning/10 text-warning",
    INFO: "bg-primary/10 text-primary",
  };
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[s] || map.INFO}`}>{formatEnergyLabel(s)}</span>;
};

const statusPill = (status) => {
  const cls = isStatus({ status }, "Open") ? "bg-error/10 text-error" : "bg-secondary/10 text-secondary";
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${cls}`}>{formatEnergyLabel(status)}</span>;
};

export default function AlertsNotifications() {
  const [alerts, setAlerts] = useState(null);
  const [filter, setFilter] = useState("All");
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  const refresh = useCallback(async () => {
    try {
      setAlerts(await getAllAlerts());
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    getAllAlerts().then(setAlerts).catch((err) => setError(err.message));
  }, []);

  async function resolve(alert) {
    setUpdatingId(alert.alertId);
    setError(null);
    try {
      await updateAlertStatus(alert.alertId, "RESOLVED");
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setUpdatingId(null);
    }
  }

  const rows = useMemo(
    () => (!alerts ? [] : filter === "All" ? alerts : alerts.filter((a) => isStatus(a, filter))),
    [alerts, filter]
  );

  if (error && !alerts) return <p className="text-sm text-error" role="alert">Failed to load alerts: {error}</p>;
  if (!alerts) return <p className="text-sm text-on-surface-variant">Loading alerts...</p>;

  const openCount = alerts.filter((a) => isStatus(a, "Open")).length;
  const criticalCount = alerts.filter((a) => a.severity === "CRITICAL").length;

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-on-background">Alerts &amp; Notifications</h1>
        <p className="mt-1 text-sm leading-relaxed text-on-surface-variant">
          {openCount} open · {criticalCount} critical requiring attention across the energy system.
        </p>
      </div>
      {error && <p className="mb-4 text-sm text-error" role="alert">{error}</p>}

      <div className="rounded-lg border border-border bg-surface-container-lowest shadow-sm border-t-[3px] border-t-primary">
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-on-background">System Alerts</h2>
          <div className="flex items-center gap-1 rounded-md bg-surface-container-low p-0.5">
            {["All", "Open", "Resolved"].map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`rounded px-3 py-1 text-xs font-semibold transition-colors ${
                  filter === f ? "bg-surface-container-lowest text-primary shadow-sm" : "text-on-surface-variant hover:text-on-background"
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
                {["Alert ID", "Severity", "Component", "Message", "Timestamp", "Status", "Action"].map((h) => (
                  <th key={h} className="border-b border-border bg-surface-container-low px-5 py-3 text-left text-xs font-bold uppercase text-on-surface-variant">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr><td colSpan={7} className="px-5 py-8 text-center text-on-surface-variant">No alerts match this filter.</td></tr>
              )}
              {rows.map((a) => (
                <tr key={a.alertId} className="hover:bg-surface-container-low">
                  <td className="border-b border-border px-5 py-3 font-semibold">{a.alertId}</td>
                  <td className="border-b border-border px-5 py-3">{severityBadge(a.severity)}</td>
                  <td className="border-b border-border px-5 py-3 whitespace-nowrap font-medium">{a.componentName || a.componentId || "—"}</td>
                  <td className="border-b border-border px-5 py-3 min-w-64 text-on-surface-variant">{a.message}</td>
                  <td className="border-b border-border px-5 py-3 whitespace-nowrap text-on-surface-variant tabular-nums">{a.timestamp}</td>
                  <td className="border-b border-border px-5 py-3">{statusPill(a.status)}</td>
                  <td className="border-b border-border px-5 py-3">
                    {isStatus(a, "Open") && <button type="button" disabled={updatingId === a.alertId}
                      onClick={() => resolve(a)}
                      className="rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary disabled:opacity-50">
                      {updatingId === a.alertId ? "Resolving..." : "Resolve"}
                    </button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}