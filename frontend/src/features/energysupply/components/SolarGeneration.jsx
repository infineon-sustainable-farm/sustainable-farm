import { useEffect, useState } from "react";
import { Sun, Zap, TrendingUp } from "lucide-react";
import { getComponents, getAllSolarLogs } from "../api/energyApi";

const statusBadge = (status) => {
  const map = {
    Operational: "bg-primary/10 text-primary",
    "Under maintenance": "bg-warning/10 text-warning",
    "Out of service": "bg-destructive/10 text-destructive",
  };
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[status] || "bg-muted-foreground/10 text-muted-foreground"}`}>
      {status}
    </span>
  );
};

function ScoreCard({ label, value, unit, icon: Icon }) {
  return (
    <div className="rounded-lg border border-border bg-card p-5 shadow-sm border-t-[3px] border-t-primary">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase tracking-[0.05em] text-muted-foreground">{label}</p>
          <p className="mt-2 font-heading text-[20px] font-bold tracking-tight text-foreground">
            {value}
            <span className="ml-1 text-base font-semibold text-muted-foreground">{unit}</span>
          </p>
        </div>
        <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
          <Icon size={20} />
        </div>
      </div>
    </div>
  );
}

export default function SolarGeneration() {
  const [arrays, setArrays] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getComponents(), getAllSolarLogs()])
      .then(([components, logs]) => {
        const pvArrays = components.filter((c) => c.category === "PV Array");
        const latestByComponent = {};
        logs.forEach((log) => {
          const existing = latestByComponent[log.componentId];
          if (!existing || log.date > existing.date) latestByComponent[log.componentId] = log;
        });
        setArrays(
          pvArrays.map((c) => ({
            ...c,
            latest: latestByComponent[c.componentId] || null,
          }))
        );
      })
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">Failed to load solar data: {error}</p>;
  if (arrays.length === 0 && !error) return <p className="text-sm text-muted-foreground">Loading solar data...</p>;

  const totalCapacity = arrays.reduce((s, a) => s + (a.capacityValue || 0), 0);
  const totalYield = arrays.reduce((s, a) => s + (a.latest?.kwhProduced || 0), 0);
  const avgEfficiency = arrays.length
    ? arrays.reduce((s, a) => s + (a.latest?.efficiencyPct || 0), 0) / arrays.length
    : 0;

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">
          Solar &amp; Generation Monitoring
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Latest output and efficiency across all PV arrays serving the mango-drying operation.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <ScoreCard label="Total Installed Capacity" value={totalCapacity.toFixed(1)} unit="kWp" icon={Sun} />
        <ScoreCard label="Latest Daily Yield" value={totalYield.toFixed(1)} unit="kWh" icon={Zap} />
        <ScoreCard label="Average Efficiency" value={avgEfficiency.toFixed(1)} unit="%" icon={TrendingUp} />
      </div>

      <div className="mt-6 rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary">
        <div className="border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Solar Array Monitoring</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr>
                {["Component ID", "Name", "Capacity (kWp)", "Latest Output (kWh)", "Efficiency (%)", "Status"].map((h) => (
                  <th key={h} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {arrays.map((a) => (
                <tr key={a.componentId} className="hover:bg-muted/40">
                  <td className="border-b border-border px-5 py-3 font-semibold">{a.componentId}</td>
                  <td className="border-b border-border px-5 py-3">{a.name}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.capacityValue?.toFixed(1)}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.latest ? a.latest.kwhProduced.toFixed(1) : "—"}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.latest ? a.latest.efficiencyPct.toFixed(1) : "—"}</td>
                  <td className="border-b border-border px-5 py-3">{statusBadge(a.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}