import { useMemo, useState } from 'react'
import { useIrrigationSchedules, useIrrigationLogs } from '../../hooks/useWaterData'
import { useZones } from '../../hooks/useFarms'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { irrigationApi } from '../../api/watersupplyApi'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'

function statusMeta(status) {
  switch ((status || '').toLowerCase()) {
    case 'running':
    case 'in_progress':
    case 'in-progress':
      return { cls: 'orange', text: 'En cours' }
    case 'completed':
    case 'done':
      return { cls: 'green', text: 'Terminé' }
    case 'postponed':
    case 'saved':
      return { cls: 'red', text: 'Reporté' }
    default:
      return { cls: 'primary', text: 'Planifié' }
  }
}

const EMPTY_SCHEDULE = { zoneId: '', startTime: '', durationMinutes: '', waterQuantityLiters: '', status: 'scheduled' }

/** Nombre de plannings actifs / cycles termines visibles dans l'interface (le reste est conserve en base). */
const VISIBLE_LIMIT = 10

/**
 * Vue Irrigation : plannings (CRUD) + logs (création manuelle, modification, suppression).
 * Liaison claire : un log référence son planning (scheduleId) ; start/stop crée/clôture le log.
 * Contrats backend : IrrigationSchedule { zoneId, startTime, durationMinutes, waterQuantityLiters, status, createdBy }
 *                   IrrigationLog { scheduleId, actualStartTime, actualEndTime, waterUsedLiters, status }.
 */
export function IrrigationView({ notify }) {
  const { schedules, loading, error, refetch } = useIrrigationSchedules()
  const { logs, loading: logsLoading, refetch: refetchLogs } = useIrrigationLogs()
  const { zones } = useZones()

  const [scheduleForm, setScheduleForm] = useState(EMPTY_SCHEDULE)
  const [editingScheduleId, setEditingScheduleId] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const updateScheduleForm = (key) => (event) => setScheduleForm((c) => ({ ...c, [key]: event.target.value }))

  const resetScheduleForm = () => {
    setEditingScheduleId(null)
    setScheduleForm(EMPTY_SCHEDULE)
    setFormError(null)
  }

  const zoneName = (zoneId) => zones.find((z) => z.id === zoneId)?.name || (zoneId ? `Zone ${String(zoneId).slice(0, 8)}` : '—')

  // Journal des irrigations : uniquement les cycles TERMINES.
  // Seuls les 10 derniers cycles sont affiches ; le 11e et les plus anciens restent conserves en base de donnees.
  const completedLogs = useMemo(
    () => logs.filter((l) => String(l.status || '').toLowerCase() === 'completed'),
    [logs],
  )
  const logList = useListControls(completedLogs, {
    searchFields: ['actualStartTime', 'actualEndTime', 'status'],
    defaultSort: { key: 'actualStartTime', dir: 'desc' },
    pageSize: VISIBLE_LIMIT,
  })

  // Plannings actifs uniquement : en cours ou planifie (les termines vont au journal), 10 plus recents.
  const activeSchedules = useMemo(
    () => schedules
      .filter((s) => String(s.status || '').toLowerCase() !== 'completed')
      .sort((a, b) => {
        const rank = (s) => ({ running: 0, in_progress: 0, scheduled: 1, postponed: 2 }[String(s.status || '').toLowerCase()] ?? 3)
        if (rank(a) !== rank(b)) return rank(a) - rank(b)
        return String(b.startTime || '').localeCompare(String(a.startTime || ''))
      })
      .slice(0, VISIBLE_LIMIT),
    [schedules],
  )

  const submitSchedule = (event) => {
    event.preventDefault()
    if (!scheduleForm.zoneId) {
      setFormError('La zone est obligatoire.')
      return
    }
    if (!scheduleForm.startTime) {
      setFormError('La date de début est obligatoire.')
      return
    }
    const duration = Number(scheduleForm.durationMinutes)
    if (!(duration > 0)) {
      setFormError('La durée doit être supérieure à 0.')
      return
    }
    const volume = Number(scheduleForm.waterQuantityLiters)
    if (!(volume > 0)) {
      setFormError('Le volume prévu doit être supérieur à 0.')
      return
    }
    setBusy(true)
    setFormError(null)
    // createdBy : requis par le backend ; le service utilise l'utilisateur système de démo si non authentifié.
    const payload = {
      zoneId: scheduleForm.zoneId,
      startTime: new Date(scheduleForm.startTime).toISOString(),
      durationMinutes: duration,
      waterQuantityLiters: volume,
      status: scheduleForm.status || 'scheduled',
    }
    const request = editingScheduleId
      ? irrigationApi.updateSchedule(editingScheduleId, payload)
      : irrigationApi.createSchedule(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingScheduleId ? 'Planning mis à jour' : 'Planning créé')
        resetScheduleForm()
      })
      .catch((err) => setFormError(err.message || 'Enregistrement impossible.'))
      .finally(() => setBusy(false))
  }

  const editSchedule = (schedule) => {
    setEditingScheduleId(schedule.id)
    setScheduleForm({
      zoneId: schedule.zoneId || '',
      startTime: (schedule.startTime || '').slice(0, 16),
      durationMinutes: schedule.durationMinutes ?? '',
      waterQuantityLiters: schedule.waterQuantityLiters ?? '',
      status: schedule.status || 'scheduled',
    })
    setFormError(null)
  }

  const handleAction = (schedule, action) => {
    const req = action === 'start' ? irrigationApi.startIrrigation(schedule.id) : irrigationApi.stopIrrigation(schedule.id)
    req
      .then(() => {
        notify(action === 'start' ? `Irrigation démarrée pour ${zoneName(schedule.zoneId)}.` : `Irrigation terminée pour ${zoneName(schedule.zoneId)}.`)
        // Le backend clôture/ouvre le log et passe le planning en "completed" : on rafraîchit les deux listes.
        refetch()
        refetchLogs()
      })
      .catch((e) => notify(e?.message || 'Action impossible.'))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Planification de l'irrigation</h1>
          <p>L'irrigation démarre quand le sol en a besoin, pas seulement parce que l'horloge le dit.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Plannings d'irrigation</h2>
              <span>{activeSchedules.length} planning(s) actif(s)</span>
            </div>
            {loading ? (
              <Spinner label="Chargement des plannings..." full />
            ) : error ? (
              <EmptyState title="Erreur" description={error.message || 'Impossible de charger.'} />
            ) : activeSchedules.length === 0 ? (
              <EmptyState
                title="Aucun planning en cours ou prévu"
                description="Créez un planning d'irrigation ci-contre. Les plannings terminés se consultent dans le Journal des irrigations."
              />
            ) : (
              activeSchedules.map((s) => {
                const st = statusMeta(s.status)
                const canStop = st.text === 'En cours'
                const canStart = st.text === 'Planifié' || st.text === 'Reporté'
                return (
                  <div className="ws-row" key={s.id}>
                    <div className="ws-row-title">
                      {zoneName(s.zoneId)}
                      <small>{(s.startTime || '').replace('T', ' ').slice(0, 16)}</small>
                    </div>
                    <div className="ws-row-detail">
                      {s.durationMinutes ?? 0} min<small>{s.waterQuantityLiters ?? 0} L prévus</small>
                    </div>
                    <span className={`ws-tag ${st.cls}`}>{st.text}</span>
                    <div className="ws-actions">
                      {canStart && (
                        <button className="ws-icon-btn" title="Démarrer maintenant" onClick={() => handleAction(s, 'start')}>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="6 3 20 12 6 21 6 3" /></svg>
                        </button>
                      )}
                      {canStop && (
                        <button className="ws-icon-btn" title="Arrêter l'irrigation" onClick={() => handleAction(s, 'stop')}>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="14" y="4" width="4" height="16" rx="1" /><rect x="6" y="4" width="4" height="16" rx="1" /></svg>
                        </button>
                      )}
                      <button className="ws-chip" onClick={() => editSchedule(s)}>Modifier</button>
                      </div>
                  </div>
                )
              })
            )}
            {schedules.length > activeSchedules.length && (
              <p style={{ margin: '4px 18px 16px', fontSize: '12px', color: 'var(--ws-muted)' }}>
                Les plannings terminés sont archivés dans le Journal des irrigations ci-dessous.
              </p>
            )}
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Journal des irrigations</h2>
              <span>{completedLogs.length} cycle(s) terminé(s)</span>
            </div>
            <div className="ws-filters">
              <SearchInput value={logList.query} onChange={logList.setQuery} placeholder="Rechercher par date (AAAA-MM-JJ)…" />
            </div>
            <div className="ws-panel-body">
              {logsLoading ? (
                <Spinner label="Chargement du journal..." />
              ) : completedLogs.length === 0 ? (
                <EmptyState
                  title="Aucun cycle terminé"
                  description="Démarrez puis arrêtez une irrigation : chaque cycle clôturé est ajouté automatiquement au journal."
                />
              ) : (
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th>Planning</th>
                      <th onClick={() => logList.toggleSort('actualStartTime')} style={{ cursor: 'pointer' }}>
                        Démarrage {logList.sort?.key === 'actualStartTime' ? (logList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Arrêt</th>
                      <th>Durée réelle</th>
                      <th onClick={() => logList.toggleSort('waterUsedLiters')} style={{ cursor: 'pointer' }}>
                        Volume réel {logList.sort?.key === 'waterUsedLiters' ? (logList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logList.items.map((l) => (
                      <tr key={l.id}>
                        <td title={l.scheduleId}>{schedules.find((s) => s.id === l.scheduleId)
                          ? zoneName(schedules.find((s) => s.id === l.scheduleId).zoneId)
                          : `#${String(l.scheduleId || '').slice(0, 8)}`}</td>
                        <td>{(l.actualStartTime || '').replace('T', ' ').slice(0, 16)}</td>
                        <td>{(l.actualEndTime || '').replace('T', ' ').slice(0, 16) || '—'}</td>
                        <td>{l.actualEndTime ? `${Math.round((new Date(l.actualEndTime) - new Date(l.actualStartTime)) / 60000)} min` : '—'}</td>
                        <td><strong>{l.waterUsedLiters ?? 0} L</strong></td>
                        <td><span className={`ws-tag ${l.status === 'completed' ? 'green' : 'orange'}`}>{l.status === 'completed' ? 'Terminé' : 'En cours'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--ws-muted)' }}>
                Seuls les {VISIBLE_LIMIT} derniers cycles terminés sont affichés ici.
                {completedLogs.length > VISIBLE_LIMIT
                  ? ` Les ${completedLogs.length - VISIBLE_LIMIT} cycle(s) plus ancien(s) restent conservés en base de données.`
                  : ' Les cycles suivants resteront également conservés en base de données.'}
              </p>
            </div>
          </div>
        </div>
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>{editingScheduleId ? 'Modifier le planning' : 'Créer un planning'}</h2>
            </div>
            <div className="ws-panel-body">
              {formError && <div className="ws-inline-error">{formError}</div>}
              <form className="ws-form-grid" onSubmit={submitSchedule}>
                <FormField label="Zone" required>
                  <select style={inputStyle} value={scheduleForm.zoneId} onChange={updateScheduleForm('zoneId')} required>
                    <option value="">Sélectionner</option>
                    {zones.map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}
                  </select>
                </FormField>
                <FormField label="Début" required>
                  <input style={inputStyle} type="datetime-local" value={scheduleForm.startTime} onChange={updateScheduleForm('startTime')} required />
                </FormField>
                <FormField label="Durée (min)" required>
                  <input style={inputStyle} type="number" min="1" step="1" value={scheduleForm.durationMinutes} onChange={updateScheduleForm('durationMinutes')} required />
                </FormField>
                <FormField label="Volume prévu (L)" required>
                  <input style={inputStyle} type="number" min="0" step="0.01" value={scheduleForm.waterQuantityLiters} onChange={updateScheduleForm('waterQuantityLiters')} required />
                </FormField>
                <FormField label="Statut">
                  <select style={inputStyle} value={scheduleForm.status} onChange={updateScheduleForm('status')}>
                    <option value="scheduled">Planifié</option>
                    <option value="postponed">Reporté</option>
                  </select>
                </FormField>
                <div className="ws-form-actions">
                  {editingScheduleId && <button type="button" className="ws-chip" onClick={resetScheduleForm}>Annuler</button>}
                  <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Enregistrer'}</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
