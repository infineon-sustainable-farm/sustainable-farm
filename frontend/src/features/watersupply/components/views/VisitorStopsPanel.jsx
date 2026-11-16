import { useState } from 'react'
import { Trash2 } from 'lucide-react'
import { visitorApi } from '../../api/watersupplyApi'
import { useZones } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useVisitorStops } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { EmptyState } from './EmptyState'

const EMPTY_STOP = { stopName: '', zoneId: '', sourceId: '' }

/**
 * Visit stops: Visitor Management names its tour stops (for example "Smart drip irrigation") and has
 * no zone or tank ids. The water team maps here the stops that pass by water facilities; the stops
 * that are not mapped are not water stops and are ignored when a visit is announced.
 */
export function VisitorStopsPanel({ notify }) {
  const { stops, loading, error, refetch } = useVisitorStops()
  const { zones } = useZones()
  const { sources } = useWaterSources()
  const [form, setForm] = useState(EMPTY_STOP)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const zoneName = (zoneId) => zones.find((zone) => zone.id === zoneId)?.name || (zoneId ? `Zone ${String(zoneId).slice(0, 8)}` : '—')
  const sourceName = (sourceId) => sources.find((source) => source.id === sourceId)?.name || (sourceId ? `Tank ${String(sourceId).slice(0, 8)}` : '—')
  const updateForm = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = (event) => {
    event.preventDefault()
    if (!form.stopName.trim()) {
      setFormError('Type the stop name exactly as Visitor Management shows it.')
      return
    }
    if (!form.zoneId && !form.sourceId) {
      setFormError('Select the zone, the tank, or both, shown at this stop.')
      return
    }
    setBusy(true)
    setFormError(null)
    visitorApi
      .createStop({ stopName: form.stopName.trim(), zoneId: form.zoneId || null, sourceId: form.sourceId || null })
      .then(() => refetch())
      .then(() => {
        notify(`Stop "${form.stopName.trim()}" mapped`)
        setForm(EMPTY_STOP)
      })
      .catch((err) => setFormError(err.message || 'Unable to save.'))
      .finally(() => setBusy(false))
  }

  const remove = (stop) => {
    visitorApi
      .deleteStop(stop.id)
      .then(() => refetch())
      .then(() => notify(`Stop "${stop.stopName}" no longer mapped`))
      .catch((err) => notify(err.message || 'Unable to delete.'))
  }

  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Visit stops</h2>
        <span>{stops.length} water stop(s)</span>
      </div>
      <div className="ws-panel-body">
        {loading ? (
          <Spinner label="Loading the stops…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load the stops.'} />
        ) : stops.length === 0 ? (
          <EmptyState title="No stop mapped yet" description="Map the Visitor Management stops that pass by water facilities." />
        ) : (
          <table className="ws-table">
            <thead>
              <tr>
                <th>Stop (Visitor Management)</th>
                <th>Zone</th>
                <th>Tank</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {stops.map((stop) => (
                <tr key={stop.id}>
                  <td><strong>{stop.stopName}</strong></td>
                  <td>{zoneName(stop.zoneId)}</td>
                  <td>{sourceName(stop.sourceId)}</td>
                  <td className="ws-table-actions">
                    <button className="ws-icon-btn danger" title="Unmap" onClick={() => remove(stop)}>
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {formError && <div className="ws-inline-error" style={{ marginTop: '14px' }}>{formError}</div>}
        <form className="ws-form-grid" onSubmit={submit} style={{ marginTop: '14px' }}>
          <FormField label="Stop name" required>
            <input style={inputStyle} value={form.stopName} onChange={updateForm('stopName')} placeholder="Smart drip irrigation" />
          </FormField>
          <FormField label="Zone shown">
            <select style={inputStyle} value={form.zoneId} onChange={updateForm('zoneId')}>
              <option value="">None</option>
              {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
            </select>
          </FormField>
          <FormField label="Tank shown">
            <select style={inputStyle} value={form.sourceId} onChange={updateForm('sourceId')}>
              <option value="">None</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
          </FormField>
          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ws-muted)' }}>
            Names are matched without regard to case. Stops not mapped here are ignored as non-water stops.
          </p>
          <div className="ws-form-actions">
            <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Map the stop'}</button>
          </div>
        </form>
      </div>
    </div>
  )
}
