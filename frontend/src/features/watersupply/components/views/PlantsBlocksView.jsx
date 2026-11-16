import { useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { zoneBlockMappingApi } from '../../api/watersupplyApi'
import { useFarms, useFields, useZones } from '../../hooks/useFarms'
import { useBlockIrrigationNeed, useBlockRainfall, useZoneBlockMappings } from '../../hooks/useIntegrations'
import { Spinner } from '../../../../shared/components/Spinner'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { EmptyState } from './EmptyState'
import { isoDay, monthStartDay } from './datetime'

const PHASES = [
  ['', 'From the Plants calendar'],
  ['establishment', 'Establishment (0–2 yrs)'],
  ['gradual production', 'Gradual production (3–5 yrs)'],
  ['full production', 'Full production (6+ yrs)'],
]

const PHASE_SOURCE_LABELS = {
  request: 'chosen here',
  plants_calendar: 'Plants calendar',
  unknown: 'unknown, no adjustment',
}

/** Plants rule: 1 to 10 letters or digits, no prefix or space ("A", "B2", never "Block A"). */
const BLOCK_CODE_PATTERN = /^[A-Za-z0-9]{1,10}$/

/**
 * Plants blocks: links Water Supply zones to the Plants blocks they water, then shows what Plants
 * reads per block (rain gauge rainfall of the month, irrigation need of the day).
 *
 * Backend contract: ZoneBlockMapping { zoneId, blockCode, farmId }. The farm is derived from the
 * zone (zone -> field -> farm) because the API refuses a farm that does not own the zone.
 */
export function PlantsBlocksView({ notify }) {
  const { mappings, loading, error, refetch } = useZoneBlockMappings()
  const { zones } = useZones()
  const { fields } = useFields()
  const { farms } = useFarms()
  const [form, setForm] = useState({ zoneId: '', blockCode: '' })
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)
  const [selectedBlock, setSelectedBlock] = useState('')

  const zoneNames = useMemo(() => new Map(zones.map((zone) => [zone.id, zone.name])), [zones])
  const zoneName = (zoneId) => zoneNames.get(zoneId) || `Zone ${String(zoneId).slice(0, 8)}`
  const farmIdOfZone = (zoneId) => {
    const fieldId = zones.find((zone) => zone.id === zoneId)?.fieldId
    return fields.find((field) => field.id === fieldId)?.farmId
  }
  const farmName = (farmId) => farms.find((farm) => farm.id === farmId)?.name || (farmId ? `Farm ${String(farmId).slice(0, 8)}` : '—')

  const blockCodes = useMemo(() => [...new Set(mappings.map((mapping) => mapping.blockCode))].sort(), [mappings])
  const sortedMappings = useMemo(
    () => [...mappings].sort((a, b) => a.blockCode.localeCompare(b.blockCode)
      || String(zoneNames.get(a.zoneId) ?? '').localeCompare(String(zoneNames.get(b.zoneId) ?? ''))),
    [mappings, zoneNames],
  )
  // A zone listed under several blocks: Plants receives multipleBlockCodes=true for its volumes.
  const sharedZones = useMemo(() => {
    const counts = new Map()
    mappings.forEach((mapping) => counts.set(mapping.zoneId, (counts.get(mapping.zoneId) || 0) + 1))
    return new Set([...counts.entries()].filter(([, count]) => count > 1).map(([zoneId]) => zoneId))
  }, [mappings])
  const activeBlock = selectedBlock || blockCodes[0] || ''

  const updateForm = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = (event) => {
    event.preventDefault()
    if (!form.zoneId) {
      setFormError('Select a zone.')
      return
    }
    if (!BLOCK_CODE_PATTERN.test(form.blockCode.trim())) {
      setFormError('The block code is 1 to 10 letters or digits, such as A or B2, without prefix or space.')
      return
    }
    const farmId = farmIdOfZone(form.zoneId)
    if (!farmId) {
      setFormError('The farm of this zone is unknown: check its field in Farms & Fields.')
      return
    }
    setBusy(true)
    setFormError(null)
    zoneBlockMappingApi
      .createMapping({ zoneId: form.zoneId, blockCode: form.blockCode.trim(), farmId })
      .then((created) => refetch().then(() => created))
      .then((created) => {
        notify(`Zone linked to block ${created?.blockCode ?? form.blockCode.trim().toUpperCase()}`)
        setForm({ zoneId: '', blockCode: '' })
      })
      .catch((err) => setFormError(err.message || 'Unable to save.'))
      .finally(() => setBusy(false))
  }

  const unlink = (mapping) => {
    zoneBlockMappingApi
      .deleteMapping(mapping.id)
      .then(() => refetch())
      .then(() => notify(`Zone unlinked from block ${mapping.blockCode}`))
      .catch((err) => notify(err.message || 'Unable to delete.'))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Plants blocks</h1>
          <p>Link each irrigated zone to the Plants block it waters: Plants then reads its water per block.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Zone ↔ block links</h2>
              <span>{blockCodes.length} block(s), {mappings.length} link(s)</span>
            </div>
            <div className="ws-panel-body">
              {loading ? (
                <Spinner label="Loading links…" full />
              ) : error ? (
                <EmptyState title="Error" description={error.message || 'Unable to load the links.'} />
              ) : mappings.length === 0 ? (
                <EmptyState title="No zone linked yet" description="Link a zone to its Plants block with the form on the right." />
              ) : (
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th>Block</th>
                      <th>Zone</th>
                      <th>Farm</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedMappings.map((mapping) => (
                      <tr key={mapping.id}>
                        <td><span className="ws-tag primary">{mapping.blockCode}</span></td>
                        <td>
                          {zoneName(mapping.zoneId)}
                          {sharedZones.has(mapping.zoneId) && (
                            <span className="ws-tag orange" style={{ marginLeft: '6px' }} title="Its volumes cannot be attributed to a single block">several blocks</span>
                          )}
                        </td>
                        <td>{farmName(mapping.farmId)}</td>
                        <td className="ws-table-actions">
                          <button className="ws-icon-btn danger" title="Unlink" onClick={() => unlink(mapping)}>
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

          {blockCodes.length > 0 && (
            <BlockDetails
              blockCodes={blockCodes}
              activeBlock={activeBlock}
              onSelect={setSelectedBlock}
            />
          )}
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Link a zone</h2>
            <Plus size={16} />
          </div>
          <div className="ws-panel-body">
            {formError && <div className="ws-inline-error">{formError}</div>}
            <form className="ws-form-grid" onSubmit={submit}>
              <FormField label="Zone" required>
                <select style={inputStyle} value={form.zoneId} onChange={updateForm('zoneId')}>
                  <option value="">Select</option>
                  {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
                </select>
              </FormField>
              <FormField label="Plants block code" required>
                <input style={inputStyle} value={form.blockCode} onChange={updateForm('blockCode')} placeholder="A, B2…" />
              </FormField>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ws-muted)' }}>
                Codes are stored in upper case. A zone watering two blocks can be linked to both,
                but a block belongs to only one zone.
              </p>
              <div className="ws-form-actions">
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Link'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}

/** What Plants reads for one block: rainfall of the current month and irrigation need of today. */
function BlockDetails({ blockCodes, activeBlock, onSelect }) {
  const [phase, setPhase] = useState('')
  const today = isoDay()
  const { data: rainfall, loading: rainLoading, error: rainError } = useBlockRainfall(activeBlock, monthStartDay(), today)
  const { data: need, loading: needLoading, error: needError } = useBlockIrrigationNeed(activeBlock, phase, today)

  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Block {activeBlock}</h2>
        <div className="ws-switcher">
          {blockCodes.map((code) => (
            <button key={code} className={`ws-chip ${code === activeBlock ? 'active' : ''}`} onClick={() => onSelect(code)}>{code}</button>
          ))}
        </div>
      </div>
      <div className="ws-panel-body">
        <div className="ws-layout-3">
          <div className="ws-row-detail">
            {rainLoading ? '…' : rainError ? '—' : <strong>{rainfall?.rainfallMm ?? 0} mm</strong>}
            <small>
              Rainfall this month
              {rainfall && ` · ${rainfall.gaugeCount} gauge(s) of the farm`}
            </small>
          </div>
          <div className="ws-row-detail">
            {needLoading ? '…' : needError ? '—' : <strong>{need?.needLiters ?? 0} L</strong>}
            <small>Irrigation need today</small>
          </div>
          <div className="ws-row-detail">
            {need ? <strong>{need.growthPhase || 'unknown'} × {need.phaseFactor}</strong> : '—'}
            <small>Growth phase ({PHASE_SOURCE_LABELS[need?.growthPhaseSource] || '…'})</small>
          </div>
        </div>
        {(rainError || needError) && (
          <div className="ws-inline-error" style={{ marginTop: '12px' }}>{(rainError || needError).message}</div>
        )}
        <div style={{ marginTop: '14px', maxWidth: '320px' }}>
          <FormField label="Growth phase used for the need">
            <select style={inputStyle} value={phase} onChange={(event) => setPhase(event.target.value)}>
              {PHASES.map(([value, label]) => <option key={label} value={value}>{label}</option>)}
            </select>
          </FormField>
        </div>
        {need?.warnings?.includes('phase_factors_not_validated') && (
          <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--ws-muted)' }}>
            Phase factors are still 1.0: they wait for the agronomists' validation, the need is the plain FAO-56 one
            (base {need.baseNeedLiters} L, ET0 {need.et0Mm} mm).
          </p>
        )}
        {rainfall && (
          <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--ws-muted)' }}>
            Rain gauges belong to the farm, not to the block: two gauges of the same farm are never added up.
          </p>
        )}
      </div>
    </div>
  )
}
