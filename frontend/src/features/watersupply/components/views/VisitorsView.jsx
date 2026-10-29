import { useState } from 'react'
import { visitorApi } from '../../api/watersupplyApi'
import { useZones } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useSafetyBriefing, useVisitorStops, useVisitorSummary } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { EmptyState } from './EmptyState'
import { DemoValvePanel } from './DemoValvePanel'
import { VisitorStopsPanel } from './VisitorStopsPanel'
import { formatInstant } from './datetime'

/** One entry per line in the form; the API takes lists. */
const toLines = (list) => (list || []).join('\n')
const fromLines = (text) => text.split('\n').map((line) => line.trim()).filter(Boolean)

/**
 * Visitors: what the water team publishes for Visitor Management and what it shows.
 *
 * The visitor screens themselves belong to Visitor Management: here the water team writes the
 * water part of the safety briefing, maps the tour stops that pass by water facilities, previews
 * the data a visitor screen receives, and drives the demonstration valve.
 */
export function VisitorsView({ notify }) {
  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Visitors</h1>
          <p>Safety rules and live figures shown to visitor groups by Visitor Management.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-stack">
          <SafetyBriefingPanel notify={notify} />
          <VisitorStopsPanel notify={notify} />
        </div>
        <div className="ws-stack">
          <VisitorPreviewPanel />
          <DemoValvePanel notify={notify} />
        </div>
      </div>
    </>
  )
}

function SafetyBriefingPanel({ notify }) {
  const { briefing, loading, error, refetch } = useSafetyBriefing()
  const [draft, setDraft] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const startEditing = () => {
    setDraft({
      version: '',
      text: briefing?.text || '',
      allowedZones: toLines(briefing?.allowedZones),
      forbiddenZones: toLines(briefing?.forbiddenZones),
      risks: toLines(briefing?.risks),
      maxCapacity: briefing?.maxCapacity ?? '',
      guideRequired: briefing?.guideRequired ?? true,
    })
    setFormError(null)
  }
  const updateDraft = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    setDraft((current) => ({ ...current, [key]: value }))
  }

  const publish = (event) => {
    event.preventDefault()
    const capacity = draft.maxCapacity === '' ? null : Number(draft.maxCapacity)
    if (capacity !== null && !(capacity >= 1)) {
      setFormError('The group size is at least 1 visitor.')
      return
    }
    setBusy(true)
    setFormError(null)
    visitorApi
      .publishSafetyBriefing({
        // Empty version: the backend dates the new version itself.
        version: draft.version.trim() || null,
        text: draft.text,
        allowedZones: fromLines(draft.allowedZones),
        forbiddenZones: fromLines(draft.forbiddenZones),
        risks: fromLines(draft.risks),
        maxCapacity: capacity,
        guideRequired: draft.guideRequired,
      })
      .then(() => refetch())
      .then(() => {
        notify('Safety briefing published')
        setDraft(null)
      })
      .catch((err) => setFormError(err.message || 'Unable to publish.'))
      .finally(() => setBusy(false))
  }

  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Water safety briefing</h2>
        {briefing && (
          <span className={`ws-tag ${briefing.validated ? 'green' : 'orange'}`}>
            {briefing.validated ? `Version ${briefing.version}` : 'Default text, not validated'}
          </span>
        )}
      </div>
      <div className="ws-panel-body">
        {loading ? (
          <Spinner label="Loading the briefing…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load the briefing.'} />
        ) : draft ? (
          <>
            {formError && <div className="ws-inline-error">{formError}</div>}
            <form className="ws-form-grid" onSubmit={publish}>
              <FormField label="Version (empty = today's date)">
                <input style={inputStyle} value={draft.version} onChange={updateDraft('version')} />
              </FormField>
              <FormField label="Text read to the group">
                <textarea style={{ ...inputStyle, minHeight: '80px', resize: 'vertical' }} value={draft.text} onChange={updateDraft('text')} />
              </FormField>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ws-muted)' }}>
                Allowed and forbidden areas must match the access rules defined by Site Security.
              </p>
              <FormField label="Allowed areas (one per line)">
                <textarea style={{ ...inputStyle, minHeight: '60px', resize: 'vertical' }} value={draft.allowedZones} onChange={updateDraft('allowedZones')} />
              </FormField>
              <FormField label="Forbidden areas (one per line)">
                <textarea style={{ ...inputStyle, minHeight: '60px', resize: 'vertical' }} value={draft.forbiddenZones} onChange={updateDraft('forbiddenZones')} />
              </FormField>
              <FormField label="Risks (one per line)">
                <textarea style={{ ...inputStyle, minHeight: '60px', resize: 'vertical' }} value={draft.risks} onChange={updateDraft('risks')} />
              </FormField>
              <FormField label="Maximum group size">
                <input style={inputStyle} type="number" min="1" step="1" value={draft.maxCapacity} onChange={updateDraft('maxCapacity')} />
              </FormField>
              <label style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '13px' }}>
                <input type="checkbox" checked={draft.guideRequired} onChange={updateDraft('guideRequired')} />
                A guide must accompany the group
              </label>
              <div className="ws-form-actions">
                <button type="button" className="ws-chip" onClick={() => setDraft(null)}>Cancel</button>
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Publish'}</button>
              </div>
            </form>
          </>
        ) : briefing ? (
          <>
            {briefing.text && <p style={{ marginTop: 0, fontSize: '14px' }}>{briefing.text}</p>}
            <BriefingList title="Allowed areas" items={briefing.allowedZones} tone="green" />
            <BriefingList title="Forbidden areas" items={briefing.forbiddenZones} tone="red" />
            <BriefingList title="Risks" items={briefing.risks} tone="orange" />
            <p style={{ fontSize: '13px' }}>
              At most <strong>{briefing.maxCapacity ?? '—'}</strong> visitors per group
              {briefing.guideRequired ? ', always with a guide.' : '.'}
            </p>
            {briefing.publishedAt && (
              <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>Published {formatInstant(briefing.publishedAt)}. Earlier versions are kept.</p>
            )}
            <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>
              Water Supply writes and validates the water content; the access areas follow the rules set by
              Site Security; Visitor Management adds this text to its own briefing and collects the signature.
            </p>
            <div className="ws-form-actions">
              <button className="ws-action-btn" onClick={startEditing}>Publish a new version</button>
            </div>
          </>
        ) : null}
      </div>
    </div>
  )
}

function BriefingList({ title, items, tone }) {
  if (!items?.length) return null
  return (
    <div style={{ marginBottom: '12px' }}>
      <div style={{ fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>{title}</div>
      <div className="ws-switcher">
        {items.map((item) => <span key={item} className={`ws-tag ${tone}`}>{item}</span>)}
      </div>
    </div>
  )
}

/** Exactly what Visitor Management receives from GET /api/water/visitor/summary. */
function VisitorPreviewPanel() {
  const { sources } = useWaterSources()
  const { zones } = useZones()
  const { stops } = useVisitorStops()
  const [stop, setStop] = useState('')
  const [sourceId, setSourceId] = useState('')
  const [zoneId, setZoneId] = useState('')
  const { summary, loading, error } = useVisitorSummary(sourceId, zoneId, stop)

  const level = summary?.sourceLevel
  const irrigation = summary?.irrigationStatus
  const live = summary?.liveConsumption
  const outages = summary?.activeUnavailability ?? []

  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Visitor screen preview</h2>
      </div>
      <div className="ws-filters">
        <select value={stop} onChange={(event) => setStop(event.target.value)} aria-label="Visit stop">
          <option value="">Any stop</option>
          {stops.map((item) => <option key={item.id} value={item.stopName}>{item.stopName}</option>)}
        </select>
        <select value={sourceId} onChange={(event) => setSourceId(event.target.value)} aria-label="Tank shown" disabled={Boolean(stop)}>
          <option value="">No tank</option>
          {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
        </select>
        <select value={zoneId} onChange={(event) => setZoneId(event.target.value)} aria-label="Zone shown" disabled={Boolean(stop)}>
          <option value="">Whole site</option>
          {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
        </select>
      </div>
      <div className="ws-panel-body">
        {loading ? (
          <Spinner label="Loading the preview…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load the preview.'} />
        ) : summary ? (
          <>
            <div className="ws-layout-3">
              <div className="ws-row-detail">
                <strong>{level ? `${level.levelPercentage ?? '—'}%` : '—'}</strong>
                <small>{level ? `${level.name} level` : 'Select a tank'}</small>
              </div>
              <div className="ws-row-detail">
                <strong>{summary.waterSavingsPercentage == null ? '—' : `${summary.waterSavingsPercentage}%`}</strong>
                <small>Water saved this month</small>
              </div>
              <div className="ws-row-detail">
                <strong>{live?.flowRateLitersPerMinute ?? 0} L/min</strong>
                <small>Flow over the last {live?.windowMinutes ?? 15} min</small>
              </div>
            </div>
            {irrigation && (
              <p style={{ fontSize: '13px' }}>
                <span className={`ws-tag ${irrigation.active ? 'orange' : 'primary'}`}>
                  {irrigation.active ? 'Irrigating now' : 'Not irrigating'}
                </span>
                {irrigation.lastActivationAt && ` Last started ${formatInstant(irrigation.lastActivationAt)}.`}
              </p>
            )}
            {outages.length > 0 && (
              <div className="ws-inline-error">
                {outages.length} planned outage(s) in force{zoneId || sourceId ? ' here' : ''}: visitors will be told water is unavailable.
              </div>
            )}
            <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>
              During a visit, Visitor Management also sends its time slot: the total shown to the group is the
              water used since the slot started.
            </p>
            {summary.waterSavingsPercentage == null && (
              <p style={{ fontSize: '12px', color: 'var(--ws-muted)' }}>Savings are unavailable while the weather service is down.</p>
            )}
          </>
        ) : null}
      </div>
    </div>
  )
}
