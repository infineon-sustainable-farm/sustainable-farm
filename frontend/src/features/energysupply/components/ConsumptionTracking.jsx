import { useEffect, useState } from "react";
import { Gauge } from "lucide-react";
import { getConsumptionToday } from "../api/energyApi";

const CHART_COLORS = ["#4caf50", "#ef6c00", "#c62828", "#4a9d93", "#7fb8b1", "#5c6b68"];

function Donut({ data, total }) {
  const radius = 70, stroke = 26, circ = 2 * Math.PI * radius;
  let offset = 0;
  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center sm:justify-center">
      <svg width="180" height="180" viewBox="0 0 180 180" role="img" aria-label="Consumption breakdown by category">
        <g transform="translate(90,90) rotate(-90)">
          {data.map((d, i) => {
            const frac = total ? d.kwhConsumed / total : 0;
            const el = (
              <circle
                key={d.loadCategory}
                r={radius}
                fill="none"
                stroke={CHART_COLORS[i % CHART_COLORS.length]}
                strokeWidth={stroke}
                strokeDasharray={`${frac * circ} ${circ - frac * circ}`}
                strokeDashoffset={-offset * circ}
              />
            );
            offset += frac;
            return el;
          })}
        </g>
        <text x="90" y="84" textAnchor="middle" fill="#1a2422" fontSize="22" fontWeight="700">{total}</text>
        <text x="90" y="102" textAnchor="middle" fill="#5c6b68" fontSize="11">kWh today</text>
      </svg>
      <ul className="grid gap-2">
        {data.map((d, i) => (
          <li key={d.loadCategory} className="flex items-center gap-2 text-sm">
            <span className="size-3 shrink-0 rounded-sm" style={{ backgroundColor: CHART_COLORS[i % CHART_COLORS.length] }}></span>
            <span className="text-foreground">{d.loadCategory}</span>
            <span className="ml-auto pl-4 font-semibold text-muted-foreground tabular-nums">
              {total ? Math.round((d.kwhConsumed / total) * 100) : 0}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default function ConsumptionTracking() {
  const [logs, setLogs] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getConsumptionToday()
      .then(setLogs)
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">Failed to load consumption data: {error}</p>;
  if (!logs) return <p className="text-sm text-muted-foreground">Loading consumption data...</p>;

  const total = logs.reduce((s, l) => s + (l.kwhConsumed || 0), 0);

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">
          Energy Consumption Tracking
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Where the farm's electricity is going today, broken down by load category.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-lg border border-border bg-card p-5 shadow-sm border-t-[3px] border-t-primary">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase tracking-[0.05em] text-muted-foreground">Total Consumption Today</p>
              <p className="mt-2 font-heading text-[20px] font-bold tracking-tight text-foreground">
                {total}<span className="ml-1 text-base font-semibold text-muted-foreground">kWh</span>
              </p>
            </div>
            <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary"><Gauge size={20} /></div>
          </div>
        </div>
        <div className="rounded-lg border border-border bg-card p-5 shadow-sm border-t-[3px] border-t-primary">
          <p className="text-xs font-medium uppercase tracking-[0.05em] text-muted-foreground">Categories Reporting</p>
          <p className="mt-2 font-heading text-[20px] font-bold tracking-tight text-foreground">{logs.length}</p>
        </div>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-5">
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-3">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Consumption by Load Category</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr>
                  {["Category", "kWh Today", "% of Total", "Source"].map((h) => (
                    <th key={h} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {logs.map((l) => (
                  <tr key={l.logId} className="hover:bg-muted/40">
                    <td className="border-b border-border px-5 py-3 font-medium">{l.loadCategory}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums">{l.kwhConsumed}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums">{total ? Math.round((l.kwhConsumed / total) * 100) : 0}%</td>
                    <td className="border-b border-border px-5 py-3">{l.source}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-2">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Load Breakdown</h2>
          </div>
          <div className="p-5">
            {logs.length > 0 ? <Donut data={logs} total={total} /> : <p className="text-sm text-muted-foreground">No data for today yet.</p>}
          </div>
        </div>
      </div>
    </section>
  );
}