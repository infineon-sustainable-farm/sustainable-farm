import { useState } from 'react'
import { Search } from 'lucide-react'
import { productTransformationApi } from '../../api/watersupplyApi'
import { useWaterSources } from '../../hooks/useWaterData'
import { useLotCompliance, useLotConsumption } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { EmptyState } from './EmptyState'
import { AvailabilityForecastPanel } from './AvailabilityForecastPanel'
import { ACTIVITIES, activityLabel } from './lotActivities'
import { formatInstant, fromLocalInput } from './datetime'

const COMPLIANCE_META = {
  compliant: { cls: 'green', text: 'Compliant' },
  non_compliant: { cls: 'red', text: 'Not compliant' },
  no_test: { cls: 'orange', text: 'No test in the window' },
}

const USAGE_WARNINGS = {
  no_consumption_found: 'No metered consumption in this window.',
  window_overlaps_other_lot: 'Some consumptions already belong to another lot: they were left untouched.',
  no_source_or_zone_filter: 'No source given: every meter of the site was considered.',
}

const EMPTY_USAGE = { lotId: '', activity: 'WASHING', sourceId: '', startAt: '', endAt: '' }

/**
 * Processing lots (Product Transformation integration): water used per lot and activity, quality
 * of that water, attribution of a metering window to a lot, and the availability forecast.
 *
 * Meters know nothing about lots: Product Transformation (or the water team here) tells which
 * time window belongs to which lot, and the consumptions of that window are attributed to it.
 */
export function LotsView({ notify }) {
  const { sources } = useWaterSources()
  const [lotQuery, setLotQuery] = useState('')
  const [activeLot, setActiveLot] = useState('')
  const [usage, setUsage] = useState(EMPTY_USAGE)
  const [usageResult, setUsageResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [usageError, setUsageError] = useState(null)

  const { data: lot, loading: lotLoading, error: lotError, refetch: refetchLot } = useLotConsumption(activeLot)
  const { data: compliance, loading: complianceLoading, error: complianceError, refetch: refetchCompliance } = useLotCompliance(activeLot)

  const sourceName = (sourceId) => sources.find((source) => source.id === sourceId)?.name || (sourceId ? `Source ${String(sourceId).slice(0, 8)}` : '—')
  const updateUsage = (key) => (event) => setUsage((current) => ({ ...current, [key]: event.target.value }))

  const lookUp = (event) => {
    event.preventDefault()
    setActiveLot(lotQuery.trim())
  }

  const attach = (event) => {
    event.preventDefault()
    if (!usage.lotId.trim()) {
      setUsageError('The lot identifier is required.')
      return
    }
    // The API accepts a window without source, but it then sweeps every meter of the site.
    if (!usage.sourceId) {
      setUsageError('Select the source used by the lot.')
      return
    }
    if (!usage.startAt || !usage.endAt || new Date(usage.endAt) <= new Date(usage.startAt)) {
      setUsageError('Give a start and an end after the start.')
      return
    }
    setBusy(true)
    setUsageError(null)
    const lotId = usage.lotId.trim()
    productTransformationApi
      .attachLotUsage({
        lotId,
        activity: usage.activity,
        sourceId: usage.sourceId,
        startAt: fromLocalInput(usage.startAt),
        endAt: fromLocalInput(usage.endAt),
      })
      .then((result) => {
        setUsageResult(result)
        notify(`${result.attachedCount} consumption(s) attributed to ${lotId}`)
        // Show the lot that was just updated.
        setLotQuery(lotId)
        if (lotId === activeLot) {
          refetchLot()
          refetchCompliance()
        } else {
          setActiveLot(lotId)
        }
      })
      .catch((err) => setUsageError(err.message || 'Unable to attribute the window.'))
      .finally(() => setBusy(false))
  }

  const verdict = compliance ? COMPLIANCE_META[compliance.status] : null

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Processing lots</h1>
          <p>Water used by each Product Transformation lot, its quality, and what the tanks can still supply.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Lot lookup</h2>
          </div>
          <form className="ws-filters" onSubmit={lookUp}>
            <input
              style={{ ...inputStyle, maxWidth: '280px' }}
              value={lotQuery}
              onChange={(event) => setLotQuery(event.target.value)}
              placeholder="Lot identifier, e.g. LOT-2026-0142"
              aria-label="Lot identifier"
            />
            <button type="submit" className="ws-action-btn" disabled={!lotQuery.trim()}>
              <Search size={14} /> Look up
            </button>
          </form>
          <div className="ws-panel-body">
            {!activeLot ? (
              <EmptyState title="No lot selected" description="Enter a lot identifier to see its water consumption and quality." />
            ) : lotLoading ? (
              <Spinner label="Loading the lot…" />
            ) : lotError ? (
              <EmptyState title="Error" description={lotError.message || 'Unable to load the lot.'} />
            ) : (
              <>
                <div className="ws-metric-banner">
                  <div className="ws-metric-pill">
                    <div>
                      <div className="label">Water used by {lot.lotId}</div>
                      <div className="value" style={{ color: 'var(--ws-primary-dark)' }}>{lot.totalLiters} L</div>
                    </div>
                  </div>
                  <div className="ws-metric-pill">
                    <div>
                      <div className="label">Washing water quality</div>
                      <div className="value">
                        {complianceLoading ? '…' : verdict
                          ? <span className={`ws-tag ${verdict.cls}`}>{verdict.text}</span>
                          : '—'}
                      </div>
                    </div>
                  </div>
                </div>
                {complianceError && (
                  <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>
                    No quality verdict: {complianceError.message}
                  </p>
                )}
                {compliance && (
                  <>
                    {/* Each source used by the lot needs its own valid test: one verdict per source. */}
                    <div className="ws-switcher" style={{ margin: '6px 0' }}>
                      {(compliance.sources || []).map((source) => {
                        const meta = COMPLIANCE_META[source.status]
                        return (
                          <span key={source.sourceId} className={`ws-tag ${meta?.cls || 'primary'}`}>
                            {sourceName(source.sourceId)}: {meta?.text || source.status}
                          </span>
                        )
                      })}
                    </div>
                    <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>
                      {compliance.tests.length} decisive test(s): for each source, the last test of the {compliance.validityDays} days
                      before the lot and the tests made during it ({formatInstant(compliance.from)} → {formatInstant(compliance.to)}) ·
                      pH {compliance.rule.phMin}–{compliance.rule.phMax}, turbidity ≤ {compliance.rule.turbidityMaxNtu} NTU.
                      {compliance.status === 'no_test' && ' A source without a valid test: the water of this lot cannot be declared compliant.'}
                    </p>
                  </>
                )}
                {lot.items.length === 0 ? (
                  <EmptyState title="No consumption for this lot" description="Attribute a metering window to this lot with the form on the right." />
                ) : (
                  <>
                    <div className="ws-switcher" style={{ margin: '6px 0 12px' }}>
                      {Object.entries(lot.byActivity).map(([activity, liters]) => (
                        <span key={activity} className="ws-tag primary">{activityLabel(activity)}: {liters} L</span>
                      ))}
                    </div>
                    <table className="ws-table">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Activity</th>
                          <th>Source</th>
                          <th>Volume</th>
                        </tr>
                      </thead>
                      <tbody>
                        {lot.items.map((item) => (
                          <tr key={item.consumptionId}>
                            <td>{formatInstant(item.consumptionDate)}</td>
                            <td>{activityLabel(item.activity)}</td>
                            <td>{sourceName(item.sourceId)}</td>
                            <td><strong>{item.liters} L</strong></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </>
                )}
              </>
            )}
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Attribute a metering window</h2>
          </div>
          <div className="ws-panel-body">
            {usageError && <div className="ws-inline-error">{usageError}</div>}
            <form className="ws-form-grid" onSubmit={attach}>
              <FormField label="Lot" required>
                <input style={inputStyle} value={usage.lotId} onChange={updateUsage('lotId')} placeholder="LOT-2026-0142" />
              </FormField>
              <FormField label="Activity" required>
                <select style={inputStyle} value={usage.activity} onChange={updateUsage('activity')}>
                  {ACTIVITIES.map(([key, label]) => <option key={key} value={key}>{label}</option>)}
                </select>
              </FormField>
              <FormField label="Source" required>
                <select style={inputStyle} value={usage.sourceId} onChange={updateUsage('sourceId')}>
                  <option value="">Select</option>
                  {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
                </select>
              </FormField>
              <FormField label="From" required>
                <input style={inputStyle} type="datetime-local" value={usage.startAt} onChange={updateUsage('startAt')} />
              </FormField>
              <FormField label="To" required>
                <input style={inputStyle} type="datetime-local" value={usage.endAt} onChange={updateUsage('endAt')} />
              </FormField>
              <div className="ws-form-actions">
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Attribute'}</button>
              </div>
            </form>
            {usageResult && (
              <div style={{ marginTop: '14px', fontSize: '13px' }}>
                <strong>{usageResult.attachedCount} consumption(s), {usageResult.attachedLiters} L</strong> attributed to {usageResult.lotId}.
                {usageResult.warnings.map((warning) => (
                  <p key={warning} style={{ margin: '6px 0 0', color: 'var(--ws-orange)' }}>{USAGE_WARNINGS[warning] || warning}</p>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <AvailabilityForecastPanel sources={sources} />
    </>
  )
}
