import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { productTransformationApi } from '../../api/watersupplyApi'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ACTIVITIES } from './lotActivities'
import { fromLocalInput } from './datetime'

const BUFFER_META = {
  ok: { cls: 'green', text: 'Enough water' },
  tight: { cls: 'orange', text: 'Below the minimum buffer' },
  insufficient: { cls: 'red', text: 'Not enough water' },
}

const EMPTY_BATCH = { lotId: '', activity: 'WASHING', sizeKg: '', plannedAt: '' }

/**
 * Availability forecast of a source for planned batches (Product Transformation): current level
 * minus the batches' demand and, by default, the scheduled irrigations of the source's farm. A tight
 * or insufficient buffer also raises an alert for the water team (backend side).
 */
export function AvailabilityForecastPanel({ sources }) {
  const [sourceId, setSourceId] = useState('')
  const [horizonDays, setHorizonDays] = useState('7')
  const [includeIrrigation, setIncludeIrrigation] = useState(true)
  const [batches, setBatches] = useState([EMPTY_BATCH])
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const updateBatch = (index, key) => (event) =>
    setBatches((current) => current.map((batch, i) => (i === index ? { ...batch, [key]: event.target.value } : batch)))

  const submit = (event) => {
    event.preventDefault()
    if (!sourceId) {
      setError('Select the source to forecast.')
      return
    }
    const horizon = Number(horizonDays)
    if (!(horizon >= 1 && horizon <= 90)) {
      setError('The horizon is between 1 and 90 days.')
      return
    }
    setBusy(true)
    setError(null)
    productTransformationApi
      .forecastAvailability({
        sourceId,
        horizonDays: horizon,
        includeIrrigation,
        batches: batches.map((batch) => ({
          lotId: batch.lotId.trim() || null,
          activity: batch.activity,
          sizeKg: batch.sizeKg === '' ? null : Number(batch.sizeKg),
          plannedAt: fromLocalInput(batch.plannedAt),
        })),
      })
      .then(setResult)
      .catch((err) => {
        setResult(null)
        setError(err.message || 'Unable to compute the forecast.')
      })
      .finally(() => setBusy(false))
  }

  const status = result ? BUFFER_META[result.bufferStatus] : null

  return (
    <div className="ws-panel" style={{ marginTop: '22px' }}>
      <div className="ws-panel-header">
        <h2>Availability forecast</h2>
        <span>Can the tank supply the planned batches?</span>
      </div>
      <div className="ws-panel-body">
        {error && <div className="ws-inline-error">{error}</div>}
        <form className="ws-form-grid" onSubmit={submit}>
          <div className="ws-layout-3">
            <FormField label="Source" required>
              <select style={inputStyle} value={sourceId} onChange={(event) => setSourceId(event.target.value)}>
                <option value="">Select</option>
                {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
              </select>
            </FormField>
            <FormField label="Horizon (days)" required>
              <input style={inputStyle} type="number" min="1" max="90" value={horizonDays} onChange={(event) => setHorizonDays(event.target.value)} />
            </FormField>
            <label style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '13px', alignSelf: 'end' }}>
              <input type="checkbox" checked={includeIrrigation} onChange={(event) => setIncludeIrrigation(event.target.checked)} />
              Include the farm's scheduled irrigations
            </label>
          </div>

          <table className="ws-table">
            <thead>
              <tr>
                <th>Lot</th>
                <th>Activity</th>
                <th>Size (kg)</th>
                <th>Planned at</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {batches.map((batch, index) => (
                <tr key={index}>
                  <td><input style={inputStyle} value={batch.lotId} onChange={updateBatch(index, 'lotId')} aria-label={`Batch ${index + 1} lot`} /></td>
                  <td>
                    <select style={inputStyle} value={batch.activity} onChange={updateBatch(index, 'activity')} aria-label={`Batch ${index + 1} activity`}>
                      {ACTIVITIES.map(([key, label]) => <option key={key} value={key}>{label}</option>)}
                    </select>
                  </td>
                  <td><input style={inputStyle} type="number" min="0" step="0.1" value={batch.sizeKg} onChange={updateBatch(index, 'sizeKg')} aria-label={`Batch ${index + 1} size`} /></td>
                  <td><input style={inputStyle} type="datetime-local" value={batch.plannedAt} onChange={updateBatch(index, 'plannedAt')} aria-label={`Batch ${index + 1} date`} /></td>
                  <td className="ws-table-actions">
                    <button
                      type="button"
                      className="ws-icon-btn danger"
                      title="Remove batch"
                      disabled={batches.length === 1}
                      onClick={() => setBatches((current) => current.filter((_, i) => i !== index))}
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="ws-form-actions">
            <button type="button" className="ws-chip" onClick={() => setBatches((current) => [...current, EMPTY_BATCH])}>
              <Plus size={12} /> Add a batch
            </button>
            <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Forecast'}</button>
          </div>
        </form>

        {result && (
          <div style={{ marginTop: '16px' }}>
            <div className="ws-metric-banner">
              <div className="ws-metric-pill"><div><div className="label">In the tank now</div><div className="value">{result.currentLevelLiters} L</div></div></div>
              <div className="ws-metric-pill"><div><div className="label">Batches</div><div className="value">{result.batchDemandLiters} L</div></div></div>
              <div className="ws-metric-pill"><div><div className="label">Irrigations</div><div className="value">{result.irrigationDemandLiters} L</div></div></div>
              <div className="ws-metric-pill">
                <div>
                  <div className="label">Left after {result.horizonDays} day(s)</div>
                  <div className="value">{result.availableAfterForecastLiters} L</div>
                </div>
              </div>
            </div>
            <p style={{ fontSize: '13px' }}>
              <span className={`ws-tag ${status?.cls || 'primary'}`}>{status?.text || result.bufferStatus}</span>
              {' '}Minimum buffer {result.bufferMinLiters} L.
            </p>
            {result.warnings.length > 0 && (
              <p style={{ fontSize: '12px', color: 'var(--ws-orange)' }}>
                Some activities have no water coefficient and count for 0 L: {result.warnings.join(', ')}.
              </p>
            )}
            {result.parametersStatus === 'unvalidated_defaults' && (
              <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>
                Litres per kg and the minimum buffer are working defaults, to be confirmed by Product Transformation.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
