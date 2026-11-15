import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, RefreshCw, Trash2 } from "lucide-react";
import {
  createComponent,
  deleteComponent,
  getComponents,
  updateComponent,
} from "../api/energyApi";
import { formatEnergyLabel } from "../utils/formatEnergyLabel";

const CATEGORIES = ["PV_ARRAY", "BATTERY", "INVERTER", "GENERATOR", "MONITORING"];
const STATUSES = ["OPERATIONAL", "UNDER_MAINTENANCE", "OUT_OF_SERVICE"];
const inputClass = "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground";

function nextComponentId(components) {
  const highestId = components.reduce((highest, component) => {
    const match = /^ENG-(\d{3})$/.exec(component.componentId);
    return match ? Math.max(highest, Number(match[1])) : highest;
  }, 0);
  return highestId < 999 ? `ENG-${String(highestId + 1).padStart(3, "0")}` : "";
}

function ComponentForm({ component, usedIds, saving, error, onClose, onSave }) {
  const [values, setValues] = useState({
    componentId: component?.componentId || nextComponentId(usedIds.map((componentId) => ({ componentId }))),
    name: component?.name || "",
    category: component?.category || "PV_ARRAY",
    capacityValue: component?.capacityValue ?? "",
    status: component?.status || "OPERATIONAL",
  });
  const [errors, setErrors] = useState({});

  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.componentId) next.componentId = "The component registry has reached its 999-ID limit.";
    if (!values.name.trim()) next.name = "Name is required.";
    if (values.capacityValue !== "" && (!Number.isFinite(Number(values.capacityValue)) || Number(values.capacityValue) < 0)) {
      next.capacityValue = "Enter a non-negative capacity.";
    }
    if (!component && usedIds.includes(values.componentId)) next.componentId = "This registry ID is already in use.";
    setErrors(next);
    if (Object.keys(next).length) return;

    const saved = await onSave({
      ...values,
      capacityValue: values.capacityValue === "" ? null : Number(values.capacityValue),
    });
    if (saved) onClose();
  }

  const field = (name, label, element) => (
    <div>
      <label className="mb-1 block text-sm font-medium" htmlFor={`component-${name}`}>{label}</label>
      {element}
      {errors[name] && <p className="mt-1 text-xs text-destructive">{errors[name]}</p>}
    </div>
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form onSubmit={submit} className="grid w-full max-w-lg gap-4 rounded-lg border border-border bg-card p-5 sm:grid-cols-2">
        <h2 className="font-heading text-lg font-bold sm:col-span-2">{component ? "Edit component" : "Add component"}</h2>
        {field("componentId", "Registry ID", <input id="component-componentId" className={inputClass}
          value={values.componentId} readOnly aria-readonly="true" />)}
        {field("name", "Name", <input id="component-name" className={inputClass} maxLength={255} value={values.name}
          onChange={(e) => setValues({ ...values, name: e.target.value })} />)}
        {field("category", "Category", <select id="component-category" className={inputClass} value={values.category}
          onChange={(e) => setValues({ ...values, category: e.target.value })}>
          {CATEGORIES.map((category) => <option key={category} value={category}>{formatEnergyLabel(category)}</option>)}
        </select>)}
        {field("capacityValue", "Capacity", <input id="component-capacityValue" className={inputClass} type="number" min="0" step="any"
          value={values.capacityValue} onChange={(e) => setValues({ ...values, capacityValue: e.target.value })} />)}
        {field("status", "Status", <select id="component-status" className={inputClass} value={values.status}
          onChange={(e) => setValues({ ...values, status: e.target.value })}>
          {STATUSES.map((status) => <option key={status} value={status}>{formatEnergyLabel(status)}</option>)}
        </select>)}
        {error && <p className="text-sm text-destructive sm:col-span-2" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 border-t border-border pt-4 sm:col-span-2">
          <button type="button" onClick={onClose} disabled={saving} className="rounded-md border border-border px-4 py-2 text-sm">Cancel</button>
          <button type="submit" disabled={saving} className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">
            {saving ? "Saving..." : component ? "Save changes" : "Add component"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default function EnergyComponents() {
  const [components, setComponents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setComponents(await getComponents());
      setError("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getComponents()
      .then(setComponents)
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  async function save(values) {
    setSaving(true);
    try {
      if (editing) await updateComponent(editing.componentId, values);
      else await createComponent(values);
      setFormOpen(false);
      await refresh();
      return true;
    } catch (requestError) {
      setError(requestError.message);
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function remove(component) {
    if (!window.confirm(`Delete component ${component.componentId}? Linked solar logs or alerts may prevent deletion.`)) return;
    setSaving(true);
    try {
      await deleteComponent(component.componentId);
      await refresh();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <div className="mb-5">
        <h1 className="font-heading text-2xl font-bold uppercase">Energy Components</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage registry components ENG-001 through ENG-005.</p>
      </div>
      {error && <div className="mb-4 flex items-center justify-between gap-3 text-sm text-destructive" role="alert">
        <span>{error}</span><button type="button" onClick={refresh} aria-label="Retry"><RefreshCw size={16} /></button>
      </div>}
      <div className="rounded-lg border border-border bg-card">
        <div className="flex items-center justify-between border-b border-border px-5 py-3">
          <h2 className="font-heading text-sm font-bold uppercase">Component registry</h2>
          <button type="button" onClick={() => { setEditing(null); setFormOpen(true); }}
            disabled={!nextComponentId(components)}
            className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50">
            <Plus size={16} /> Add component
          </button>
        </div>
        {components.length >= 5 && nextComponentId(components) && (
          <p className="border-b border-border px-5 py-2 text-xs text-muted-foreground">
            The original ENG-001 to ENG-005 registry is populated; new components continue from {nextComponentId(components)}.
          </p>
        )}
        {loading ? <p className="p-5 text-sm text-muted-foreground">Loading components...</p> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead><tr>{["ID", "Name", "Category", "Capacity", "Status", "Actions"].map((item) =>
                <th key={item} className="border-b border-border bg-muted/50 px-4 py-3 text-xs uppercase text-muted-foreground">{item}</th>)}</tr></thead>
              <tbody>
                {components.map((component) => <tr key={component.componentId}>
                  <td className="border-b border-border px-4 py-3 font-semibold">{component.componentId}</td>
                  <td className="border-b border-border px-4 py-3">{component.name}</td>
                  <td className="border-b border-border px-4 py-3">{formatEnergyLabel(component.category)}</td>
                  <td className="border-b border-border px-4 py-3">{component.capacityValue ?? "—"}</td>
                  <td className="border-b border-border px-4 py-3">{formatEnergyLabel(component.status)}</td>
                  <td className="border-b border-border px-4 py-3">
                    <button type="button" aria-label={`Edit ${component.componentId}`} disabled={saving}
                      onClick={() => { setEditing(component); setFormOpen(true); }} className="mr-2 text-primary"><Pencil size={16} /></button>
                    <button type="button" aria-label={`Delete ${component.componentId}`} disabled={saving}
                      onClick={() => remove(component)} className="text-destructive"><Trash2 size={16} /></button>
                  </td>
                </tr>)}
                {!components.length && <tr><td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">No components configured.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {formOpen && <ComponentForm component={editing} usedIds={components.map((c) => c.componentId)} error={error}
        saving={saving} onClose={() => setFormOpen(false)} onSave={save} />}
    </section>
  );
}
