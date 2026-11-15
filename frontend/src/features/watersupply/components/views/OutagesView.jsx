import { useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { outageApi } from '../../api/watersupplyApi'
import { useFarms, useZones } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useOutages } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'
import { EmptyState } from './EmptyState'
import { formatInstant, fromLocalInput, toLocalInput } from './datetime'

const SCOPES = [
  ['zone', 'Zone'],
  ['source', 'Water source'],
  ['farm', 'Whole farm'],
]

const EMPTY_FORM = { scope: 'zone', scopeId: '', startAt: '', endAt: '', reason: '', status: 'planned', postponeIrrigations: true }

/** In force now / upcoming / finished or cancelled: computed on the client from the period. */
function phaseOf(outage, now) {
  if (outage.status === 'cancelled') return { key: 'cancelled', cls: 'red', text: 'Cancelled' }
  const start = new Date(outage.startAt).getTime()
  const end = new Date(outage.endAt).getTime()
  if (start <= now && now < end) return { key: 'active', cls: 'orange', text: 'In force' }
  if (start > now) return { key: 'upcoming', cls: 'primary', text: 'Upcoming' }
  return { key: 'past', cls: 'green', text: 'Finished' }
}

/**
 * Planned water unavailability (outage or maintenance) on a zone, a source or a whole farm.
 *
 * These periods are read by Visitor Management, refuse valve openings and skip the automatic
 * irrigation; a farm-wide period applies to every zone of the farm. Backend contract:
 * WaterUnavailability { scope, scopeId, startAt, endAt, reason, status } (+ postponeIrrigations
 * on write, to postpone the irrigations planned during the period).
 */
export function OutagesView({ notify }) {
  const { outages, loading, error, refetch } = useOutages()
  const { farms } = useFarms()
  const { sources } = useWaterSources()
  const { zones } = useZones()
  const [filter, setFilter] = useState('current')
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const targetsByScope = { zone: zones, source: sources, farm: farms }
  const targetName = (scope, scopeId) =>
    (targetsByScope[scope] || []).find((item) => item.id === scopeId)?.name || `${scope} ${String(scopeId).slice(0, 8)}`

  // Reference instant of the screen, taken once: every phase of the list is computed against it.
  const [now] = useState(() => Date.now())
  const visible = useMemo(() => outages
    .map((outage) => ({ ...outage, phase: phaseOf(outage, now) }))
    .filter((outage) => filter === 'all' || (filter === 'current'
      ? ['active', 'upcoming'].includes(outage.phase.key)
      : outage.phase.key === filter))
    .sort((a, b) => String(a.startAt).localeCompare(String(b.startAt))), [outages, filter, now])
  const activeCount = outages.filter((outage) => phaseOf(outage, now).key === 'active').length

  const updateForm = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    // Changing the scope invalidates the selected target (a zone id is not a farm id).
    setForm((current) => ({ ...current, [key]: value, ...(key === 'scope' ? { scopeId: '' } : {}) }))
  }

  const resetForm = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  const payloadOf = (values) => ({
    scope: values.scope,
    scopeId: values.scopeId,
    startAt: fromLocalInput(values.startAt),
    endAt: fromLocalInput(values.endAt),
    reason: values.reason.trim() || null,
    status: values.status,
    postponeIrrigations: values.scope !== 'source' && values.postponeIrrigations,
  })

  const submit = (event) => {
    event.preventDefault()
    if (!form.scopeId) {
      setFormError('Select what is unavailable.')
      return
    }
    if (!form.startAt || !form.endAt) {
      setFormError('Start and end are required.')
      return
    }
    if (new Date(form.endAt) <= new Date(form.startAt)) {
      setFormError('The end must be after the start.')
      return
    }
    setBusy(true)
    setFormError(null)
    const payload = payloadOf(form)
    const request = editingId ? outageApi.updateOutage(editingId, payload) : outageApi.createOutage(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingId ? 'Unavailability updated' : 'Unavailability planned')
        resetForm()
      })
      // The backend refuses an overlap on the same target: its message is shown as is.
      .catch((err) => setFormError(err.message || 'Unable to save.'))
      .finally(() => setBusy(false))
  }

  const edit = (outage) => {
    setEditingId(outage.id)
    setForm({
      scope: outage.scope,
      scopeId: outage.scopeId,
      startAt: toLocalInput(outage.startAt),
      endAt: toLocalInput(outage.endAt),
      reason: outage.reason || '',
      status: outage.status || 'planned',
      postponeIrrigations: false,
    })
    setFormError(null)
  }

  const cancel = (outage) => {
    const values = { scope: outage.scope, scopeId: outage.scopeId, startAt: outage.startAt, endAt: outage.endAt, reason: outage.reason || '' }
    outageApi
      .updateOutage(outage.id, { ...values, status: 'cancelled', postponeIrrigations: false })
      .then(() => refetch())
      .then(() => notify('Unavailability cancelled'))
      .catch((err) => notify(err.message || 'Unable to cancel.'))
  }

  const remove = () => {
    if (!deleteTarget) return
    setBusy(true)
    outageApi
      .deleteOutage(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify('Unavailability deleted')
        setDeleteTarget(null)
      })
      .catch((err) => notify(err.message || 'Unable to delete.'))
      .finally(() => setBusy(false))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Planned outages</h1>
          <p>Maintenance or cuts announced in advance: visitors are warned, valves and automatic irrigation stay off.</p>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div>
            <div className="label">In force now</div>
            <div className="value" style={{ color: activeCount ? 'var(--ws-orange)' : undefined }}>{activeCount}</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Planned periods</div>
            <div className="value">{outages.filter((outage) => outage.status !== 'cancelled').length}</div>
          </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Unavailability periods</h2>
            <div className="ws-switcher">
              {[['current', 'Current & upcoming'], ['past', 'Finished'], ['cancelled', 'Cancelled'], ['all', 'All']].map(([key, label]) => (
                <button key={key} className={`ws-chip ${filter === key ? 'active' : ''}`} onClick={() => setFilter(key)}>{label}</button>
              ))}
            </div>
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading planned outages…" full />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load planned outages.'} />
            ) : visible.length === 0 ? (
              <EmptyState title="No planned outage" description="Plan a maintenance or a cut on the right: visitors and automatic irrigation will take it into account." />
            ) : (
              <table className="ws-table">
                <thead>
                  <tr>
                    <th>What</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((outage) => (
                    <tr key={outage.id}>
                      <td>
                        <strong>{targetName(outage.scope, outage.scopeId)}</strong>
                        <div style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>{SCOPES.find(([key]) => key === outage.scope)?.[1]}</div>
                      </td>
                      <td>{formatInstant(outage.startAt)}</td>
                      <td>{formatInstant(outage.endAt)}</td>
                      <td>{outage.reason || '—'}</td>
                      <td><span className={`ws-tag ${outage.phase.cls}`}>{outage.phase.text}</span></td>
                      <td className="ws-table-actions">
                        <button className="ws-chip" onClick={() => edit(outage)}>Edit</button>
                        {outage.status !== 'cancelled' && outage.phase.key !== 'past' && (
                          <button className="ws-chip" onClick={() => cancel(outage)}>Cancel</button>
                        )}
                        <button className="ws-icon-btn danger" title="Delete" onClick={() => setDeleteTarget(outage)}>
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>{editingId ? 'Edit unavailability' : 'Plan an unavailability'}</h2>
            <Plus size={16} />
          </div>
          <div className="ws-panel-body">
            {formError && <div className="ws-inline-error">{formError}</div>}
            <form className="ws-form-grid" onSubmit={submit}>
              <FormField label="Unavailable" required>
                <select style={inputStyle} value={form.scope} onChange={updateForm('scope')}>
                  {SCOPES.map(([key, label]) => <option key={key} value={key}>{label}</option>)}
                </select>
              </FormField>
              <FormField label={SCOPES.find(([key]) => key === form.scope)[1]} required>
                <select style={inputStyle} value={form.scopeId} onChange={updateForm('scopeId')}>
                  <option value="">Select</option>
                  {(targetsByScope[form.scope] || []).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </select>
              </FormField>
              <FormField label="Start" required>
                <input style={inputStyle} type="datetime-local" value={form.startAt} onChange={updateForm('startAt')} />
              </FormField>
              <FormField label="End" required>
                <input style={inputStyle} type="datetime-local" value={form.endAt} onChange={updateForm('endAt')} />
              </FormField>
              <FormField label="Reason">
                <input style={inputStyle} value={form.reason} onChange={updateForm('reason')} placeholder="Filter cleaning, pump repair…" />
              </FormField>
              {editingId && (
                <FormField label="Status">
                  <select style={inputStyle} value={form.status} onChange={updateForm('status')}>
                    <option value="planned">Planned</option>
                    <option value="active">Active</option>
                    <option value="cancelled">Cancelled</option>
                  </select>
                </FormField>
              )}
              {form.scope === 'source' ? (
                <p style={{ margin: 0, fontSize: '12px', color: 'var(--ws-muted)' }}>
                  A source is not linked to zones: its irrigations cannot be postponed automatically.
                </p>
              ) : (
                <label style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '13px' }}>
                  <input type="checkbox" checked={form.postponeIrrigations} onChange={updateForm('postponeIrrigations')} />
                  Postpone the irrigations planned during this period
                </label>
              )}
              <div className="ws-form-actions">
                {editingId && <button type="button" className="ws-chip" onClick={resetForm}>Cancel</button>}
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>

      {deleteTarget && (
        <ConfirmDialog
          title="Delete this unavailability"
          message="The period is removed from the history. To keep a trace, cancel it instead."
          onConfirm={remove}
          onCancel={() => setDeleteTarget(null)}
          busy={busy}
        />
      )}
    </>
  )
}
