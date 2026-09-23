import { useEffect, useState } from "react";
import { Power, Clock, Fuel } from "lucide-react";
import { getComponents, getGeneratorEvents } from "../api/energyApi";

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

function isThisMonth(dateStr) {
  const d = new Date(dateStr);
  const now = new Date();
  return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
}

export default function GeneratorBackup() {
  const [generator, setGenerator] = useState(null);
  const [events, setEvents] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getComponents(), getGeneratorEvents()])
      .then(([components, evts]) => {
        setGenerator(components.find((c) => c.category === "Generator") || null);
        setEvents(evts);
      })
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">Failed to load generator data: {error}</p>;
  if (!events) return <p className="text-sm text-muted-foreground">Loading generator data...</p>;

  const monthEvents = events.filter((e) => isThisMonth(e.date));
  const runtimeMonth = monthEvents.reduce((s, e) => s + (e.durationHours || 0), 0);
  const fuelMonth = monthEvents.reduce((s, e) => s + (e.fuelLiters || 0), 0);

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-[24px] font-bold uppercase tracking-[0.05em] text-foreground">
          Generator Backup Management
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
          Diesel backup readiness and run-time history during grid outages or low-battery events.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ScoreCard label="Generator Capacity" value={generator?.capacityValue ?? "—"} unit="kW" icon={Power} />
        <ScoreCard label="Current Status" value={generator?.status ?? "—"} unit="" icon={Power} />
        <ScoreCard label="Runtime (This Month)" value={runtimeMonth.toFixed(1)} unit="hrs" icon={Clock} />
        <ScoreCard label="Fuel Used (This Month)" value={fuelMonth.toFixed(1)} unit="L" icon={Fuel} />
      </div>

      <div className="mt-6 rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary">
        <div className="border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">Backup Events Log</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr>
                {["Event ID", "Date", "Trigger Reason", "Duration (hrs)", "Fuel (L)", "Cost (EUR)"].map((h) => (
                  <th key={h} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {events.length === 0 && (
                <tr><td colSpan={6} className="px-5 py-8 text-center text-sm text-muted-foreground">No backup events recorded.</td></tr>
              )}
              {events.map((e) => (
                <tr key={e.eventId} className="hover:bg-muted/40">
                  <td className="border-b border-border px-5 py-3 font-semibold">{e.eventId}</td>
                  <td className="border-b border-border px-5 py-3 whitespace-nowrap text-muted-foreground">{e.date}</td>
                  <td className="border-b border-border px-5 py-3">{e.triggerReason}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{e.durationHours?.toFixed(1)}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{e.fuelLiters?.toFixed(1)}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums font-medium">€{e.costEur?.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <p className="mt-3 text-xs text-muted-foreground">
        Note: the "Critical Loads Covered" and "Payback vs Full Diesel" panels from the original mock-up aren't included here —
        there's no backing table for them in the current data model (Generator Event only tracks trigger, duration, fuel, cost).
      </p>
    </section>
  );
}
