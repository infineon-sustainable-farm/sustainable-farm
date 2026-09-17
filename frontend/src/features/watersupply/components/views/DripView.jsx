import { useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { dripMaintenanceApi } from '../../api/watersupplyApi'
import { useDripMaintenanceLogs } from '../../hooks/useWaterData'
import { useZones } from '../../hooks/useFarms'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'

const DAY_MS = 24 * 3600 * 1000
const MAINTENANCE_TYPES = ['cleaning', 'filter_change', 'repair', 'inspection']

function severityTag(severity) {
  switch ((severity || '').toLowerCase()) {
    case 'high': return 'red'
    case 'medium': return 'orange'
    default: return 'green'
  }
}

/**
 * Vue Goutte-à-goutte : interventions de maintenance (CRUD complet).
 * Contrat backend : DripMaintenanceLog { zoneId, maintenanceDate, maintenanceType,
 * filterCleaned, cloggingDetected, cloggingSeverity, emitterReplacedCount, notes, performedBy }.
 */
export function DripView({ notify }) {
  const { logs, loading, error, refetch } = useDripMaintenanceLogs()
  const { zones } = useZones()
  const [period, setPeriod] = useState('all')
  const [zoneFilter, setZoneFilter] = useState('')
  const [form, setForm] = useState({
    zoneId: '',
    maintenanceType: 'cleaning',
    maintenanceDate: '',
    filterCleaned: false,
    cloggingDetected: false,
    cloggingSeverity: '',
    emitterReplacedCount: '',
    notes: '',
    performedBy: '',
  })
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  // Filtre basé sur la date max des données (fonction pure — pas de Date.now() pendant le rendu).
  const maxDateMs = useMemo(() => {
    let max = 0
    for (const item of logs) {
      const time = new Date(item.maintenanceDate || item.createdAt).getTime()
      if (!Number.isNaN(time) && time > max) max = time
    }
    return max
  }, [logs])

  const zoneFiltered = useMemo(() => {
    let list = logs.filter((item) => !zoneFilter || item.zoneId === zoneFilter)
    if (period !== 'tout') {
      const days = period === 'semaine' ? 7 : 30
      const cutoff = maxDateMs - days * DAY_MS
      list = list.filter((item) => new Date(item.maintenanceDate || item.createdAt).getTime() >= cutoff)
    }
    return list
  }, [logs, zoneFilter, period, maxDateMs])

  // Recherche + tri + pagination côté client (tri par date décroissante par défaut).
  const list = useListControls(zoneFiltered, {
    searchFields: ['maintenanceType', 'notes', 'performedBy'],
    defaultSort: { key: 'maintenanceDate', dir: 'desc' },
  })
  const filtered = list.items

  const zoneName = (zoneId) => zones.find((z) => z.id === zoneId)?.name || (zoneId ? `Zone ${String(zoneId).slice(0, 8)}` : '—')

  const updateForm = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    setForm((current) => ({
      ...current,
      [key]: value,
      ...(key === 'cloggingDetected' && !event.target.checked ? { cloggingSeverity: '' } : {}),
    }))
  }

  const resetForm = () => {
    setEditingId(null)
    setForm({
      zoneId: '',
      maintenanceType: 'cleaning',
      maintenanceDate: '',
      filterCleaned: false,
      cloggingDetected: false,
      cloggingSeverity: '',
      emitterReplacedCount: '',
      notes: '',
      performedBy: '',
    })
    setFormError(null)
  }

  const submitLog = (event) => {
    event.preventDefault()
    if (!form.zoneId) {
      setFormError('La zone est obligatoire.')
      return
    }
    if (!form.maintenanceDate) {
      setFormError("La date d'intervention est obligatoire.")
      return
    }
    setBusy(true)
    setFormError(null)
    const payload = {
      zoneId: form.zoneId,
      maintenanceType: form.maintenanceType,
      maintenanceDate: new Date(form.maintenanceDate).toISOString(),
      filterCleaned: form.filterCleaned,
      cloggingDetected: form.cloggingDetected,
      cloggingSeverity: form.cloggingDetected ? form.cloggingSeverity || 'low' : null,
      emitterReplacedCount: form.emitterReplacedCount === '' ? 0 : Number(form.emitterReplacedCount),
      notes: form.notes || null,
      performedBy: form.performedBy || null,
    }
    const request = editingId ? dripMaintenanceApi.updateLog(editingId, payload) : dripMaintenanceApi.createLog(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingId ? 'Intervention mise à jour' : 'Intervention enregistrée')
        resetForm()
      })
      .catch((err) => setFormError(err.message || 'Enregistrement impossible.'))
      .finally(() => setBusy(false))
  }

  const editLog = (item) => {
    setEditingId(item.id)
    setForm({
      zoneId: item.zoneId || '',
      maintenanceType: item.maintenanceType || 'cleaning',
      maintenanceDate: (item.maintenanceDate || '').slice(0, 16),
      filterCleaned: !!item.filterCleaned,
      cloggingDetected: !!item.cloggingDetected,
      cloggingSeverity: item.cloggingSeverity || '',
      emitterReplacedCount: item.emitterReplacedCount ?? '',
      notes: item.notes || '',
      performedBy: item.performedBy || '',
    })
    setFormError(null)
  }

  const deleteLog = () => {
    if (!deleteTarget) return
    setBusy(true)
    dripMaintenanceApi
      .deleteLog(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify('Intervention supprimée')
        setDeleteTarget(null)
      })
      .catch((err) => setFormError(err.message || 'Suppression impossible.'))
      .finally(() => setBusy(false))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Maintenance </h1>
          <p>Suivi des interventions et planification de l'entretien des émetteurs.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Historique des interventions</h2>
              <div className="ws-switcher">
                {['semaine', 'mois', 'tout'].map((item) => (
                  <button key={item} className={`ws-chip ${period === item ? 'active' : ''}`} onClick={() => setPeriod(item)}>{item}</button>
                ))}
              </div>
            </div>
            <div className="ws-filters">
              <select value={zoneFilter} onChange={(event) => setZoneFilter(event.target.value)}>
                <option value="">Toutes les zones</option>
                {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
              </select>
              <SearchInput value={list.query} onChange={list.setQuery} placeholder="Rechercher une intervention…" />
            </div>
            <div className="ws-panel-body">
              {loading ? (
                <Spinner label="Chargement des interventions..." full />
              ) : error ? (
                <EmptyState title="Erreur" description={error.message || 'Impossible de charger les interventions.'} />
              ) : filtered.length === 0 ? (
                <EmptyState title="Aucune intervention" description="Enregistrez une intervention de maintenance pour démarrer l'historique." />
              ) : (
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th onClick={() => list.toggleSort('maintenanceDate')} style={{ cursor: 'pointer' }}>
                        Date {list.sort?.key === 'maintenanceDate' ? (list.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Zone</th>
                      <th onClick={() => list.toggleSort('maintenanceType')} style={{ cursor: 'pointer' }}>
                        Type {list.sort?.key === 'maintenanceType' ? (list.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>État</th>
                      <th>Émetteurs</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map((item) => (
                      <tr key={item.id}>
                        <td>{(item.maintenanceDate || '').replace('T', ' ').slice(0, 16)}</td>
                        <td>{zoneName(item.zoneId)}</td>
                        <td>{item.maintenanceType || '—'}</td>
                        <td>
                          {item.cloggingDetected ? (
                            <span className={`ws-tag ${severityTag(item.cloggingSeverity)}`}>Colmatage {item.cloggingSeverity || ''}</span>
                          ) : (
                            <span className="ws-tag green">OK</span>
                          )}
                        </td>
                        <td>{item.emitterReplacedCount ?? 0}</td>
                        <td className="ws-table-actions">
                          <button className="ws-chip" onClick={() => editLog(item)}>Modifier</button>
                          <button className="ws-icon-btn danger" title="Supprimer" onClick={() => setDeleteTarget(item)}>
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <Pagination page={list.page} pageCount={list.pageCount} onPage={list.setPage} total={list.total} unit="intervention" />
            </div>
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Planning de maintenance</h2></div>
            <div className="ws-panel-body">
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--ws-muted)', lineHeight: 1.6 }}>
                Recommandation : contrôlez les filtres chaque semaine et remplacez les émetteurs colmatés.
                {(() => {
                  const recent = logs
                    .filter((item) => new Date(item.maintenanceDate || item.createdAt).getTime() >= maxDateMs - 7 * DAY_MS)
                    .length
                  return ` ${recent} intervention(s) sur les 7 derniers jours (relatif à la dernière donnée).`
                })()}
              </p>
            </div>
          </div>
        </div>
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>{editingId ? "Modifier l'intervention" : 'Nouvelle intervention'}</h2>
            <Plus size={16} />
          </div>
          <div className="ws-panel-body">
            {formError && <div className="ws-inline-error">{formError}</div>}
            <form className="ws-form-grid" onSubmit={submitLog}>
              <FormField label="Zone" required>
                <select style={inputStyle} value={form.zoneId} onChange={updateForm('zoneId')} required>
                  <option value="">Sélectionner</option>
                  {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
                </select>
              </FormField>
              <FormField label="Type d'intervention" required>
                <select style={inputStyle} value={form.maintenanceType} onChange={updateForm('maintenanceType')} required>
                  {MAINTENANCE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                </select>
              </FormField>
              <FormField label="Date d'intervention" required>
                <input style={inputStyle} type="datetime-local" value={form.maintenanceDate} onChange={updateForm('maintenanceDate')} required />
              </FormField>
              <FormField label="Filtre nettoyé">
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
                  <input type="checkbox" checked={form.filterCleaned} onChange={updateForm('filterCleaned')} /> Oui
                </label>
              </FormField>
              <FormField label="Colmatage détecté">
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}>
                  <input type="checkbox" checked={form.cloggingDetected} onChange={updateForm('cloggingDetected')} /> Oui
                </label>
              </FormField>
              {form.cloggingDetected && (
                <FormField label="Gravité du colmatage">
                  <select style={inputStyle} value={form.cloggingSeverity} onChange={updateForm('cloggingSeverity')}>
                    <option value="">Sélectionner</option>
                    <option value="low">Faible</option>
                    <option value="medium">Moyenne</option>
                    <option value="high">Élevée</option>
                  </select>
                </FormField>
              )}
              <FormField label="Émetteurs remplacés">
                <input style={inputStyle} type="number" min="0" step="1" value={form.emitterReplacedCount} onChange={updateForm('emitterReplacedCount')} />
              </FormField>
              <FormField label="Effectué par">
                <input style={inputStyle} value={form.performedBy} onChange={updateForm('performedBy')} />
              </FormField>
              <FormField label="Notes">
                <textarea style={{ ...inputStyle, minHeight: '70px', resize: 'vertical' }} value={form.notes} onChange={updateForm('notes')} />
              </FormField>
              <div className="ws-form-actions">
                {editingId && <button type="button" className="ws-chip" onClick={resetForm}>Annuler</button>}
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Enregistrer'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
      {deleteTarget && (
        <ConfirmDialog
          title="Supprimer l'intervention"
          message="Cette intervention sera retirée de l'historique de maintenance."
          onConfirm={deleteLog}
          onCancel={() => setDeleteTarget(null)}
          busy={busy}
        />
      )}
    </>
  )
}