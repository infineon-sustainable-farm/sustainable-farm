import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Sun, Trash2, TrendingUp, Zap } from "lucide-react";
import {
  createSolarLog,
  deleteSolarLog,
  getComponents,
  getAllSolarLogs,
  updateSolarLog,
} from "../api/energyApi";
import { formatEnergyLabel } from "../utils/formatEnergyLabel";

const inputClass = "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground";
const today = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

function SolarLogForm({ log, arrays, saving, error, onClose, onSave }) {
  const [values, setValues] = useState({
    componentId: log?.componentId || arrays[0]?.componentId || "",
    date: log?.date || today(),
    kwhProduced: log?.kwhProduced ?? "",
    efficiencyPct: log?.efficiencyPct ?? "",
  });
  const [errors, setErrors] = useState({});
  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.componentId) next.componentId = "Choose a solar component.";
    if (!values.date) next.date = "Date is required.";
    else if (values.date > today()) next.date = "Date cannot be in the future.";
    for (const field of ["kwhProduced", "efficiencyPct"]) {
      if (field === "efficiencyPct" && values[field] === "") continue;
      if (values[field] === "" || !Number.isFinite(Number(values[field])) || Number(values[field]) < 0) {
        next[field] = "Enter a non-negative value.";
      }
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    const saved = await onSave({
      ...values,
      kwhProduced: Number(values.kwhProduced),
      efficiencyPct: values.efficiencyPct === "" ? null : Number(values.efficiencyPct),
    });
    if (saved) onClose();
  }
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form onSubmit={submit} className="grid w-full max-w-lg gap-4 rounded-lg border border-border bg-card p-5 sm:grid-cols-2">
        <h2 className="font-heading text-lg font-bold sm:col-span-2">{log ? "Edit solar log" : "Add solar log"}</h2>
        <label className="grid gap-1 text-sm sm:col-span-2">PV component<select className={inputClass} value={values.componentId}
          onChange={(e) => setValues({ ...values, componentId: e.target.value })}>
          {arrays.map((array) => <option key={array.componentId} value={array.componentId}>{array.componentId} · {array.name}</option>)}
        </select>{errors.componentId && <span className="text-xs text-destructive">{errors.componentId}</span>}</label>
        <label className="grid gap-1 text-sm">Date<input className={inputClass} type="date" max={today()} value={values.date}
          onChange={(e) => setValues({ ...values, date: e.target.value })} />{errors.date && <span className="text-xs text-destructive">{errors.date}</span>}</label>
        <label className="grid gap-1 text-sm">Energy generated (kWh)<input className={inputClass} type="number" min="0" step="any"
          value={values.kwhProduced} onChange={(e) => setValues({ ...values, kwhProduced: e.target.value })} />
          {errors.kwhProduced && <span className="text-xs text-destructive">{errors.kwhProduced}</span>}</label>
        <label className="grid gap-1 text-sm sm:col-span-2">Efficiency (%)<input className={inputClass} type="number" min="0" step="any"
          max="100"
          value={values.efficiencyPct} onChange={(e) => setValues({ ...values, efficiencyPct: e.target.value })} />
          {errors.efficiencyPct && <span className="text-xs text-destructive">{errors.efficiencyPct}</span>}</label>
        {error && <p className="text-sm text-destructive sm:col-span-2" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 border-t border-border pt-4 sm:col-span-2">
          <button type="button" disabled={saving} onClick={onClose} className="rounded-md border border-border px-4 py-2 text-sm">Cancel</button>
          <button disabled={saving} className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">{saving ? "Saving..." : "Save"}</button>
        </div>
      </form>
    </div>
  );
}

const statusBadge = (status) => {
  const map = {
    OPERATIONAL: "bg-primary/10 text-primary",
    UNDER_MAINTENANCE: "bg-warning/10 text-warning",
    OUT_OF_SERVICE: "bg-destructive/10 text-destructive",
  };
  const label = formatEnergyLabel(status);
  return <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[status] || "bg-muted-foreground/10 text-muted-foreground"}`}>{label}</span>;
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
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const [components, solarLogs] = await Promise.all([getComponents(), getAllSolarLogs()]);
      setLogs(solarLogs);
      const pvArrays = components.filter((c) => c.category === "PV_ARRAY");
      const latestByComponent = {};
      solarLogs.forEach((log) => {
        const componentId = log.componentId;
        const existing = latestByComponent[componentId];
        if (componentId && (!existing || log.date > existing.date)) latestByComponent[componentId] = log;
      });
      setArrays(pvArrays.map((c) => ({ ...c, latest: latestByComponent[c.componentId] || null })));
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([getComponents(), getAllSolarLogs()])
      .then(([components, solarLogs]) => {
        setLogs(solarLogs);
        const pvArrays = components.filter((component) => component.category === "PV_ARRAY");
        const latestByComponent = {};
        solarLogs.forEach((log) => {
          const existing = latestByComponent[log.componentId];
          if (log.componentId && (!existing || log.date > existing.date)) {
            latestByComponent[log.componentId] = log;
          }
        });
        setArrays(pvArrays.map((component) => ({
          ...component,
          latest: latestByComponent[component.componentId] || null,
        })));
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function save(values) {
    setSaving(true);
    setError(null);
    try {
      if (editing) await updateSolarLog(editing.logId, values);
      else await createSolarLog(values);
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
    if (!window.confirm(`Delete the solar generation log from ${log.date}?`)) return;
    setSaving(true);
    setError(null);
    try {
      await deleteSolarLog(log.logId);
      await refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (error && !arrays.length && !logs.length) return <p className="text-sm text-destructive" role="alert">Failed to load solar data: {error}</p>;
  if (loading) return <p className="text-sm text-muted-foreground">Loading solar data...</p>;

  const totalCapacity = arrays.reduce((s, a) => s + (Number(a.capacityValue) || 0), 0);
  const totalYield = arrays.reduce((s, a) => s + (Number(a.latest?.kwhProduced) || 0), 0);
  const avgEfficiency = arrays.length
    ? arrays.reduce((s, a) => s + (Number(a.latest?.efficiencyPct) || 0), 0) / arrays.length
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

      {error && <p className="mb-4 text-sm text-destructive" role="alert">{error}</p>}

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
              {arrays.length === 0 ? (
                <tr><td colSpan={6} className="px-5 py-8 text-center text-sm text-muted-foreground">No solar arrays configured yet.</td></tr>
              ) : arrays.map((a) => (
                <tr key={a.componentId} className="hover:bg-muted/40">
                  <td className="border-b border-border px-5 py-3 font-semibold">{a.componentId}</td>
                  <td className="border-b border-border px-5 py-3">{a.name}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.capacityValue?.toFixed(1)}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.latest ? a.latest.kwhProduced.toFixed(1) : "—"}</td>
                  <td className="border-b border-border px-5 py-3 tabular-nums">{a.latest?.efficiencyPct == null ? "—" : Number(a.latest.efficiencyPct).toFixed(1)}</td>
                  <td className="border-b border-border px-5 py-3">{statusBadge(a.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <div className="mt-6 rounded-lg border border-border bg-card">
        <div className="flex items-center justify-between border-b border-border px-5 py-3.5">
          <h2 className="font-heading text-[13px] font-bold uppercase">Solar generation logs</h2>
          <button type="button" disabled={!arrays.length} onClick={() => { setEditing(null); setFormOpen(true); }}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50">
            <Plus size={16} /> Add log
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead><tr>{["Date", "PV component", "kWh generated", "Efficiency (%)", "Actions"].map((item) =>
              <th key={item} className="border-b border-border bg-muted/50 px-4 py-3 text-xs uppercase text-muted-foreground">{item}</th>)}</tr></thead>
            <tbody>
              {logs.map((log) => <tr key={log.logId}>
                <td className="border-b border-border px-4 py-3">{log.date}</td>
                <td className="border-b border-border px-4 py-3">{log.componentName || log.componentId}</td>
                <td className="border-b border-border px-4 py-3">{log.kwhProduced}</td>
                <td className="border-b border-border px-4 py-3">{log.efficiencyPct ?? "—"}</td>
                <td className="border-b border-border px-4 py-3">
                  <button type="button" aria-label={`Edit solar log ${log.logId}`} disabled={saving}
                    onClick={() => { setEditing(log); setFormOpen(true); }} className="mr-2 text-primary"><Pencil size={16} /></button>
                  <button type="button" aria-label={`Delete solar log ${log.logId}`} disabled={saving}
                    onClick={() => remove(log)} className="text-destructive"><Trash2 size={16} /></button>
                </td>
              </tr>)}
              {!logs.length && <tr><td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">No solar logs recorded.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
      {formOpen && <SolarLogForm log={editing} arrays={arrays} saving={saving} error={error}
        onClose={() => setFormOpen(false)} onSave={save} />}
    </section>
  );
}