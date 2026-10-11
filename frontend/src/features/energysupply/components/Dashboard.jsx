import { useEffect, useState } from "react";
import { CloudSun, Gauge, Zap, Leaf } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import { getDashboardSummary, getComponents, getSolarLast7Days } from "../api/energyApi";

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

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [components, setComponents] = useState([]);
  const [weekly, setWeekly] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getDashboardSummary(), getComponents(), getSolarLast7Days()])
      .then(([summaryData, componentsData, solarLogs]) => {
        setSummary(summaryData);
        setComponents(componentsData);
        setWeekly(
          solarLogs.map((log) => ({
            day: log.date,
            solar: Number(log.kwhProduced ?? 0),
          }))
        );
      })
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return <p className="text-sm text-destructive">Failed to load dashboard: {error}</p>;
  }

  if (!summary) {
    return <p className="text-sm text-muted-foreground">Loading dashboard...</p>;
  }

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">
          System Overview
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Real-time snapshot of the farm's energy supply — solar generation, consumption, backup, and emissions.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ScoreCard label="Total PV Capacity" value={summary.totalPvCapacityKwp ?? 0} unit="kWp" icon={CloudSun} />
        <ScoreCard label="Daily Consumption" value={summary.currentDailyConsumptionKwh ?? 0} unit="kWh/day" icon={Gauge} />
        <ScoreCard label="Generator Status" value={summary.generatorStatus ?? "N/A"} unit="" icon={Zap} />
        <ScoreCard label="CO₂ Avoided (Month)" value={summary.co2AvoidedThisMonthKg ?? 0} unit="kg" icon={Leaf} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-5">
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-3">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">
              Solar Production · Last 7 Days
            </h2>
          </div>
          <div className="p-5" style={{ height: 240 }}>
            {weekly.length === 0 ? (
              <p className="text-sm text-muted-foreground">No solar readings recorded yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={weekly}>
                  <XAxis dataKey="day" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="solar" fill="#4caf50" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-2">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">
              System Status Overview
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr>
                  <th className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">
                    Component
                  </th>
                  <th className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">
                    Value
                  </th>
                  <th className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody>
                {components.map((c) => (
                  <tr key={c.componentId} className="hover:bg-muted/40">
                    <td className="border-b border-border px-5 py-3">
                      <div className="font-semibold">{c.name}</div>
                      <div className="text-xs text-muted-foreground">{c.category}</div>
                    </td>
                    <td className="border-b border-border px-5 py-3 text-sm">
                      {c.capacityValue ?? "-"}
                    </td>
                    <td className="border-b border-border px-5 py-3">
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                        {c.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  );
}
