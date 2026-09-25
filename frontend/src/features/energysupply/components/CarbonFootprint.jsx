import { useEffect, useState } from "react";
import { Leaf, CalendarRange } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { getCarbonMetrics } from "../api/energyApi";

function ScoreCard({ label, value, unit, icon: Icon }) {
  return (
    <div className="rounded-lg border border-border bg-card p-5 shadow-sm border-t-[3px] border-t-primary">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase tracking-[0.05em] text-muted-foreground">{label}</p>
          <p className="mt-2 font-heading text-[20px] font-bold tracking-tight text-foreground">
            {value}<span className="ml-1 text-base font-semibold text-muted-foreground">{unit}</span>
          </p>
        </div>
        <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
          <Icon size={20} />
        </div>
      </div>
    </div>
  );
}

export default function CarbonFootprint() {
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getCarbonMetrics()
      .then((data) => setMetrics([...data].sort((a, b) => a.period.localeCompare(b.period))))
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">Failed to load carbon data: {error}</p>;
  if (!metrics) return <p className="text-sm text-muted-foreground">Loading carbon data...</p>;

  const latest = metrics[metrics.length - 1] || null;
  const currentYear = new Date().getFullYear().toString();
  const ytd = metrics
    .filter((m) => m.period.startsWith(currentYear))
    .reduce((s, m) => s + (m.co2AvoidedKg || 0), 0);

  let cumulative = 0;
  const chartData = metrics.map((m) => {
    cumulative += m.co2AvoidedKg || 0;
    return { period: m.period, cumulativeKg: cumulative };
  });

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">Carbon Footprint</h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Emissions avoided by running the farm on solar instead of the grid or diesel generator.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <ScoreCard label="CO₂ Avoided (Latest Period)" value={latest?.co2AvoidedKg ?? "—"} unit="kg" icon={Leaf} />
        <ScoreCard label="CO₂ Avoided (YTD)" value={ytd} unit="kg" icon={CalendarRange} />
        <ScoreCard label="Scenario (Latest)" value={latest?.scenario ?? "—"} unit="" icon={Leaf} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-5">
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-3">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Cumulative CO₂ Avoided</h2>
          </div>
          <div className="p-5" style={{ height: 260 }}>
            {chartData.length === 0 ? (
              <p className="text-sm text-muted-foreground">No carbon metrics recorded yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid stroke="#dbe3e1" vertical={false} />
                  <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip formatter={(v) => [`${v} kg`, "Cumulative CO₂"]} />
                  <Line type="monotone" dataKey="cumulativeKg" stroke="#4caf50" strokeWidth={2.5} dot={{ r: 3.5, fill: "#4caf50" }} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
        <div className="rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary xl:col-span-2">
          <div className="border-b border-border px-5 py-3.5">
            <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Carbon Metrics by Period</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr>
                  {["Period", "Scenario", "CO₂ Avoided (kg)", "Emission Factor"].map((h) => (
                    <th key={h} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {metrics.map((m) => (
                  <tr key={m.metricId} className="hover:bg-muted/40">
                    <td className="border-b border-border px-5 py-3 whitespace-nowrap">{m.period}</td>
                    <td className="border-b border-border px-5 py-3">{m.scenario}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums font-medium">{m.co2AvoidedKg}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums">{m.emissionFactorUsed}</td>
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