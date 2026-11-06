import { useCallback, useEffect, useState } from "react";
import { Gauge, Pencil, Plus, Trash2 } from "lucide-react";
import {
  createConsumptionLog,
  deleteConsumptionLog,
  getAllConsumption,
  getConsumptionToday,
  updateConsumptionLog,
} from "../api/energyApi";
import { formatEnergyLabel } from "../utils/formatEnergyLabel";

const CHART_COLORS = ["var(--color-primary)", "var(--color-secondary)", "#ef6c00", "#c62828", "#7fb8b1", "#5c6b68"];
const CATEGORIES = ["IRRIGATION", "STORAGE_COOLING", "LIGHTING", "SECURITY", "MONITORING", "PROCESSING"];
const SOURCES = ["SOLAR", "BATTERY", "GENERATOR", "GRID"];
const inputClass = "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground";
const today = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

function ConsumptionForm({ log, saving, error, onClose, onSave }) {
  const [values, setValues] = useState({
    date: log?.date || today(),
    loadCategory: log?.loadCategory || "IRRIGATION",
    kwhConsumed: log?.kwhConsumed ?? "",
    source: log?.source || "",
  });
  const [errors, setErrors] = useState({});
  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.date) next.date = "Date is required.";
    if (values.date > today()) next.date = "Date cannot be in the future.";
    if (values.kwhConsumed === "" || !Number.isFinite(Number(values.kwhConsumed)) || Number(values.kwhConsumed) < 0) {
      next.kwhConsumed = "Enter a non-negative kWh value.";
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    const saved = await onSave({
      ...values,
      kwhConsumed: Number(values.kwhConsumed),
      source: values.source || null,
    });
    if (saved) onClose();
  }
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form onSubmit={submit} className="grid w-full max-w-lg gap-4 rounded-lg border border-border bg-card p-5 sm:grid-cols-2">
        <h2 className="font-heading text-lg font-bold sm:col-span-2">{log ? "Edit consumption log" : "Add consumption log"}</h2>
        <label className="grid gap-1 text-sm">Date<input className={inputClass} type="date" max={today()}
          value={values.date} onChange={(e) => setValues({ ...values, date: e.target.value })} />{errors.date && <span className="text-xs text-destructive">{errors.date}</span>}</label>
        <label className="grid gap-1 text-sm">Load category<select className={inputClass} value={values.loadCategory}
          onChange={(e) => setValues({ ...values, loadCategory: e.target.value })}>
          {CATEGORIES.map((item) => <option key={item} value={item}>{formatEnergyLabel(item)}</option>)}
        </select></label>
        <label className="grid gap-1 text-sm">Energy (kWh)<input className={inputClass} type="number" min="0" step="any"
          value={values.kwhConsumed} onChange={(e) => setValues({ ...values, kwhConsumed: e.target.value })} />{errors.kwhConsumed && <span className="text-xs text-destructive">{errors.kwhConsumed}</span>}</label>
        <label className="grid gap-1 text-sm">Source<select className={inputClass} value={values.source}
          onChange={(e) => setValues({ ...values, source: e.target.value })}>
          <option value="">Unspecified</option>
          {SOURCES.map((item) => <option key={item} value={item}>{formatEnergyLabel(item)}</option>)}
        </select></label>
        {error && <p className="text-sm text-destructive sm:col-span-2" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 border-t border-border pt-4 sm:col-span-2">
          <button type="button" disabled={saving} onClick={onClose} className="rounded-md border border-border px-4 py-2 text-sm">Cancel</button>
          <button disabled={saving} className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">{saving ? "Saving..." : "Save"}</button>
        </div>
      </form>
    </div>
  );
}

function Donut({ data, total }) {
  const radius = 70, stroke = 26, circ = 2 * Math.PI * radius;
  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center sm:justify-center">
      <svg width="180" height="180" viewBox="0 0 180 180" role="img" aria-label="Consumption breakdown by category">
        <g transform="translate(90,90) rotate(-90)">
          {data.map((d, i) => {
            const frac = total ? d.kwhConsumed / total : 0;
            const offset = data
              .slice(0, i)
              .reduce((sum, item) => sum + (total ? item.kwhConsumed / total : 0), 0);

            return (
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
          })}
        </g>
        <text x="90" y="84" textAnchor="middle" fill="#1a2422" fontSize="22" fontWeight="700">{total}</text>
        <text x="90" y="102" textAnchor="middle" fill="#5c6b68" fontSize="11">kWh today</text>
      </svg>
      <ul className="grid gap-2">
        {data.map((d, i) => (
          <li key={d.loadCategory} className="flex items-center gap-2 text-sm">
            <span className="size-3 shrink-0 rounded-sm" style={{ backgroundColor: CHART_COLORS[i % CHART_COLORS.length] }}></span>
            <span className="text-foreground">{formatEnergyLabel(d.loadCategory)}</span>
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
  const [allLogs, setAllLogs] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);

  const refresh = useCallback(async () => {
    try {
      const [today, all] = await Promise.all([getConsumptionToday(), getAllConsumption()]);
      setLogs(today);
      setAllLogs(all);
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    Promise.all([getConsumptionToday(), getAllConsumption()])
      .then(([today, all]) => {
        setLogs(today);
        setAllLogs(all);
      })
      .catch((err) => setError(err.message));
  }, []);

  async function save(values) {
    setSaving(true);
    setError(null);
    try {
      if (editing) await updateConsumptionLog(editing.logId, values);
      else await createConsumptionLog(values);
      setFormOpen(false);
      await refresh();
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function remove(log) {
    if (!window.confirm(`Delete the consumption log from ${log.date}?`)) return;
    setSaving(true);
    setError(null);
    try {
      await deleteConsumptionLog(log.logId);
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (error && !logs) return <p className="text-sm text-destructive" role="alert">Failed to load consumption data: {error}</p>;
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
      {error && <p className="mb-4 text-sm text-destructive" role="alert">{error}</p>}

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
                    <td className="border-b border-border px-5 py-3 font-medium">{formatEnergyLabel(l.loadCategory)}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums">{l.kwhConsumed}</td>
                    <td className="border-b border-border px-5 py-3 tabular-nums">{total ? Math.round((l.kwhConsumed / total) * 100) : 0}%</td>
                    <td className="border-b border-border px-5 py-3">{formatEnergyLabel(l.source)}</td>
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
      <div className="mt-6 rounded-lg border border-border bg-card">
        <div className="flex items-center justify-between border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase">Consumption logs</h2>
          <button type="button" onClick={() => { setEditing(null); setFormOpen(true); }}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground">
            <Plus size={16} /> Add log
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead><tr>{["Date", "Category", "kWh", "Source", "Actions"].map((item) =>
              <th key={item} className="border-b border-border bg-muted/50 px-4 py-3 text-xs uppercase text-muted-foreground">{item}</th>)}</tr></thead>
            <tbody>
              {allLogs.map((log) => <tr key={log.logId}>
                <td className="border-b border-border px-4 py-3">{log.date}</td>
                <td className="border-b border-border px-4 py-3">{formatEnergyLabel(log.loadCategory)}</td>
                <td className="border-b border-border px-4 py-3">{log.kwhConsumed}</td>
                <td className="border-b border-border px-4 py-3">{formatEnergyLabel(log.source)}</td>
                <td className="border-b border-border px-4 py-3">
                  <button type="button" aria-label={`Edit log ${log.logId}`} disabled={saving}
                    onClick={() => { setEditing(log); setFormOpen(true); }} className="mr-2 text-primary"><Pencil size={16} /></button>
                  <button type="button" aria-label={`Delete log ${log.logId}`} disabled={saving}
                    onClick={() => remove(log)} className="text-destructive"><Trash2 size={16} /></button>
                </td>
              </tr>)}
              {!allLogs.length && <tr><td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">No consumption logs.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
      {formOpen && <ConsumptionForm log={editing} saving={saving} error={error}
        onClose={() => setFormOpen(false)} onSave={save} />}
    </section>
  );
}