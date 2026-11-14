import { useEffect, useState } from "react";
import { Clock, Fuel, Pencil, Plus, Power, RefreshCw, Trash2 } from "lucide-react";
import { getComponents } from "../api/energyApi";
import GeneratorEventFormModal from "./GeneratorEventFormModal";
import { useGeneratorEvents } from "../hooks/useGeneratorEvents";
import { formatEnergyLabel } from "../utils/formatEnergyLabel";

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

export default function GeneratorBackup() {
  const [generator, setGenerator] = useState(null);
  const [generatorError, setGeneratorError] = useState(null);
  const [generatorLoading, setGeneratorLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const { events, summary, loading, saving, error, refresh, saveEvent, removeEvent } = useGeneratorEvents();

  useEffect(() => {
    let active = true;
    getComponents()
      .then((components) => {
        if (active) setGenerator(components.find((component) => component.category === "GENERATOR") || null);
      })
      .catch((requestError) => {
        if (active) setGeneratorError(requestError.message);
      })
      .finally(() => {
        if (active) setGeneratorLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading || generatorLoading) {
    return <p className="text-sm text-muted-foreground">Loading generator data...</p>;
  }

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

      {generatorError && (
        <p className="mb-4 text-sm text-destructive" role="alert">
          Failed to load generator component: {generatorError}
        </p>
      )}
      {error && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-md border border-destructive/30 bg-destructive/5 px-4 py-3">
          <p className="text-sm text-destructive" role="alert">Generator events: {error}</p>
          <button type="button" onClick={() => refresh().catch(() => {})}
            className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline">
            <RefreshCw size={15} /> Retry
          </button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ScoreCard label="Generator Capacity" value={generator?.capacityValue ?? "—"} unit="kW" icon={Power} />
        <ScoreCard label="Current Status" value={formatEnergyLabel(generator?.status)} unit="" icon={Power} />
        <ScoreCard label="Runtime (This Month)" value={Number(summary?.runtimeHoursThisMonth ?? 0).toFixed(1)} unit="hrs" icon={Clock} />
        <ScoreCard label="Fuel Used (This Month)" value={Number(summary?.fuelLitersThisMonth ?? 0).toFixed(1)} unit="L" icon={Fuel} />
      </div>

      <div className="mt-6 rounded-lg border border-border bg-card shadow-sm border-t-[3px] border-t-primary">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase tracking-[0.05em] text-foreground">
            Backup Events Log
          </h2>
          <button type="button" onClick={() => { setEditingEvent(null); setFormOpen(true); }}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90">
            <Plus size={16} /> Add event
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-[13px]">
            <thead>
              <tr>
                {["Date", "Trigger Reason", "Duration (hrs)", "Fuel (L)", "Cost (EUR)", "Actions"].map((heading) => (
                  <th key={heading} className="border-b border-border bg-muted/50 px-5 py-3 text-left text-xs font-bold uppercase text-muted-foreground">
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {events.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-8 text-center text-sm text-muted-foreground">
                    No backup events recorded.
                  </td>
                </tr>
              )}
              {events.map((event) => (
                <tr key={event.eventId} className="hover:bg-muted/40">
                  <td className="whitespace-nowrap border-b border-border px-5 py-3 text-muted-foreground">{event.date}</td>
                  <td className="border-b border-border px-5 py-3">{event.triggerReason || "—"}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">
                    {event.durationHours == null ? "not recorded" : Number(event.durationHours).toFixed(1)}
                  </td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">
                    {event.fuelLiters == null ? "not recorded" : Number(event.fuelLiters).toFixed(1)}
                  </td>
                  <td className="border-b border-border px-5 py-3 font-medium tabular-nums">
                    {event.costEur == null ? "not recorded" : `€${Number(event.costEur).toFixed(2)}`}
                  </td>
                  <td className="border-b border-border px-5 py-3">
                    <div className="flex items-center gap-1">
                      <button type="button" aria-label={`Edit event from ${event.date}`} disabled={saving}
                        onClick={() => { setEditingEvent(event); setFormOpen(true); }}
                        className="rounded-md p-2 text-primary hover:bg-primary/10 disabled:opacity-50">
                        <Pencil size={16} />
                      </button>
                      <button type="button" aria-label={`Delete event from ${event.date}`} disabled={saving}
                        onClick={() => {
                          if (window.confirm(`Delete the backup event from ${event.date}?`)) {
                            removeEvent(event.eventId).catch(() => {});
                          }
                        }}
                        className="rounded-md p-2 text-destructive hover:bg-destructive/10 disabled:opacity-50">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
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
      {formOpen && (
        <GeneratorEventFormModal
          event={editingEvent}
          saving={saving}
          error={error}
          onClose={() => setFormOpen(false)}
          onSave={saveEvent}
        />
      )}
    </section>
  );
}
