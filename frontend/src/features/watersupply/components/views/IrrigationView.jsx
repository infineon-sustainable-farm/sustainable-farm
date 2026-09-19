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
      return { cls: 'orange', text: 'Running' }
    case 'completed':
    case 'done':
      return { cls: 'green', text: 'Completed' }
    case 'postponed':
    case 'saved':
      return { cls: 'red', text: 'Postponed' }
    default:
      return { cls: 'primary', text: 'Scheduled' }
  }
}

const EMPTY_SCHEDULE = { zoneId: '', startTime: '', durationMinutes: '', waterQuantityLiters: '', status: 'scheduled' }

/** Number of active schedules / completed cycles shown in the UI (the rest stays in the database). */
const VISIBLE_LIMIT = 10

/**
 * Irrigation view: schedules (CRUD) + logs (manual creation, edit, delete).
 * Clear binding: a log references its schedule (scheduleId); start/stop creates/closes the log.
 * Backend contracts: IrrigationSchedule { zoneId, startTime, durationMinutes, waterQuantityLiters, status, createdBy }
 *                    IrrigationLog { scheduleId, actualStartTime, actualEndTime, waterUsedLiters, status }.
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

  // Irrigation journal: only COMPLETED cycles.
  // Only the last 10 cycles are displayed; the 11th and older ones stay stored in the database.
  const completedLogs = useMemo(
    () => logs.filter((l) => String(l.status || '').toLowerCase() === 'completed'),
    [logs],
  )
  const logList = useListControls(completedLogs, {
    searchFields: ['actualStartTime', 'actualEndTime', 'status'],
    defaultSort: { key: 'actualStartTime', dir: 'desc' },
    pageSize: VISIBLE_LIMIT,
  })

  // Active schedules only: running or scheduled (completed ones go to the journal), 10 most recent.
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
      setFormError('Zone is required.')
      return
    }
    if (!scheduleForm.startTime) {
      setFormError('Start date is required.')
      return
    }
    const duration = Number(scheduleForm.durationMinutes)
    if (!(duration > 0)) {
      setFormError('Duration must be greater than 0.')
      return
    }
    const volume = Number(scheduleForm.waterQuantityLiters)
    if (!(volume > 0)) {
      setFormError('Planned volume must be greater than 0.')
      return
    }
    setBusy(true)
    setFormError(null)
    // createdBy: required by the backend; the service falls back to the demo system user when unauthenticated.
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
        notify(editingScheduleId ? 'Schedule updated' : 'Schedule created')
        resetScheduleForm()
      })
      .catch((err) => setFormError(err.message || 'Unable to save.'))
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
        notify(action === 'start' ? `Irrigation started for ${zoneName(schedule.zoneId)}.` : `Irrigation stopped for ${zoneName(schedule.zoneId)}.`)
        // The backend closes/opens the log and sets the schedule to "completed": refresh both lists.
        refetch()
        refetchLogs()
      })
      .catch((e) => notify(e?.message || 'Action failed.'))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Irrigation scheduling</h1>
          <p>Irrigation starts when the soil needs it, not just because the clock says so.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Irrigation schedules</h2>
              <span>{activeSchedules.length} active schedule(s)</span>
            </div>
            {loading ? (
              <Spinner label="Loading schedules…" full />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load.'} />
            ) : activeSchedules.length === 0 ? (
              <EmptyState
                title="No running or scheduled plans"
                description="Create an irrigation schedule on the right. Completed schedules are listed in the Irrigation journal."
              />
            ) : (
              activeSchedules.map((s) => {
                const st = statusMeta(s.status)
                const status = String(s.status || '').toLowerCase()
                const canStop = ['running', 'in_progress', 'in-progress'].includes(status)
                const canStart = ['scheduled', 'postponed', 'saved'].includes(status)
                return (
                  <div className="ws-row" key={s.id}>
                    <div className="ws-row-title">
                      {zoneName(s.zoneId)}
                      <small>{(s.startTime || '').replace('T', ' ').slice(0, 16)}</small>
                    </div>
                    <div className="ws-row-detail">
                      {s.durationMinutes ?? 0} min<small>{s.waterQuantityLiters ?? 0} L planned</small>
                    </div>
                    <span className={`ws-tag ${st.cls}`}>{st.text}</span>
                    <div className="ws-actions">
                      {canStart && (
                        <button className="ws-icon-btn" title="Start now" onClick={() => handleAction(s, 'start')}>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="6 3 20 12 6 21 6 3" /></svg>
                        </button>
                      )}
                      {canStop && (
                        <button className="ws-icon-btn" title="Stop irrigation" onClick={() => handleAction(s, 'stop')}>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="14" y="4" width="4" height="16" rx="1" /><rect x="6" y="4" width="4" height="16" rx="1" /></svg>
                        </button>
                      )}
                      <button className="ws-chip" onClick={() => editSchedule(s)}>Edit</button>
                      </div>
                  </div>
                )
              })
            )}
            {schedules.length > activeSchedules.length && (
              <p style={{ margin: '4px 18px 16px', fontSize: '12px', color: 'var(--ws-muted)' }}>
                Completed schedules are archived in the Irrigation journal below.
              </p>
            )}
          </div>
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Irrigation journal</h2>
              <span>{completedLogs.length} completed cycle(s)</span>
            </div>
            <div className="ws-filters">
              <SearchInput value={logList.query} onChange={logList.setQuery} placeholder="Search by date (YYYY-MM-DD)…" />
            </div>
            <div className="ws-panel-body">
              {logsLoading ? (
                <Spinner label="Loading journal…" />
              ) : completedLogs.length === 0 ? (
                <EmptyState
                  title="No completed cycles"
                  description="Start then stop an irrigation: each closed cycle is added to the journal automatically."
                />
              ) : (
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th>Schedule</th>
                      <th onClick={() => logList.toggleSort('actualStartTime')} style={{ cursor: 'pointer' }}>
                        Start {logList.sort?.key === 'actualStartTime' ? (logList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Stop</th>
                      <th>Actual duration</th>
                      <th onClick={() => logList.toggleSort('waterUsedLiters')} style={{ cursor: 'pointer' }}>
                        Actual volume {logList.sort?.key === 'waterUsedLiters' ? (logList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Status</th>
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
                        <td><span className={`ws-tag ${l.status === 'completed' ? 'green' : 'orange'}`}>{l.status === 'completed' ? 'Completed' : 'Running'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--ws-muted)' }}>
                Only the last {VISIBLE_LIMIT} completed cycles are shown here.
                {completedLogs.length > VISIBLE_LIMIT
                  ? ` Older cycles remain stored in the database.`
                  : ' Additional cycles will also remain stored in the database.'}
              </p>
            </div>
          </div>
        </div>
        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>{editingScheduleId ? 'Edit schedule' : 'Create a schedule'}</h2>
            </div>
            <div className="ws-panel-body">
              {formError && <div className="ws-inline-error">{formError}</div>}
              <form className="ws-form-grid" onSubmit={submitSchedule}>
                <FormField label="Zone" required>
                  <select style={inputStyle} value={scheduleForm.zoneId} onChange={updateScheduleForm('zoneId')} required>
                    <option value="">Select</option>
                    {zones.map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}
                  </select>
                </FormField>
                <FormField label="Start" required>
                  <input style={inputStyle} type="datetime-local" value={scheduleForm.startTime} onChange={updateScheduleForm('startTime')} required />
                </FormField>
                <FormField label="Duration (min)" required>
                  <input style={inputStyle} type="number" min="1" step="1" value={scheduleForm.durationMinutes} onChange={updateScheduleForm('durationMinutes')} required />
                </FormField>
                <FormField label="Planned volume (L)" required>
                  <input style={inputStyle} type="number" min="0" step="0.01" value={scheduleForm.waterQuantityLiters} onChange={updateScheduleForm('waterQuantityLiters')} required />
                </FormField>
                <FormField label="Status">
                  <select style={inputStyle} value={scheduleForm.status} onChange={updateScheduleForm('status')}>
                    <option value="scheduled">Scheduled</option>
                    <option value="postponed">Postponed</option>
                  </select>
                </FormField>
                <div className="ws-form-actions">
                  {editingScheduleId && <button type="button" className="ws-chip" onClick={resetScheduleForm}>Cancel</button>}
                  <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Save'}</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
