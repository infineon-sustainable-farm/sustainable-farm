import { useMemo, useState } from 'react'
import { Pause, Pencil, Play } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, ErrorState, Field, IconButton, Input, Select, Table } from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { useIrrigationSchedules, useIrrigationLogs } from '../../hooks/useWaterData'
import { useZones } from '../../hooks/useFarms'
import { irrigationApi } from '../../api/watersupplyApi'
import { useWaterFormat } from '../../format'
import { Filters, FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { tableSort } from '../common/listSort'

/** Status label key and badge tone of a schedule. */
function statusMeta(status) {
  switch ((status || '').toLowerCase()) {
    case 'running':
    case 'in_progress':
    case 'in-progress':
      return { tone: 'warning', key: 'running' }
    case 'completed':
    case 'done':
      return { tone: 'success', key: 'completed' }
    case 'postponed':
    case 'saved':
      return { tone: 'error', key: 'postponed' }
    default:
      return { tone: 'info', key: 'scheduled' }
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
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
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

  const zoneName = (zoneId) =>
    zones.find((z) => z.id === zoneId)?.name || (zoneId ? t('irrigation.zoneFallback', { id: String(zoneId).slice(0, 8) }) : t('common.none'))
  const instant = (iso) => (iso ? format.instant(iso) : '')

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
      setFormError(t('irrigation.errors.zoneRequired'))
      return
    }
    if (!scheduleForm.startTime) {
      setFormError(t('irrigation.errors.startRequired'))
      return
    }
    const duration = Number(scheduleForm.durationMinutes)
    if (!(duration > 0)) {
      setFormError(t('irrigation.errors.durationPositive'))
      return
    }
    const volume = Number(scheduleForm.waterQuantityLiters)
    if (!(volume > 0)) {
      setFormError(t('irrigation.errors.volumePositive'))
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
        notify(editingScheduleId ? t('irrigation.updated') : t('irrigation.created'))
        resetScheduleForm()
      })
      .catch((err) => setFormError(err.message || t('common.saveError')))
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
        const zone = zoneName(schedule.zoneId)
        notify(action === 'start' ? t('irrigation.started', { zone }) : t('irrigation.stopped', { zone }))
        // The backend closes/opens the log and sets the schedule to "completed": refresh both lists.
        refetch()
        refetchLogs()
      })
      .catch((e) => notify(e?.message || t('irrigation.actionFailed'), 'error'))
  }

  const scheduleOf = (log) => schedules.find((s) => s.id === log.scheduleId)

  const journalColumns = [
    {
      key: 'schedule',
      header: t('irrigation.journal.columns.schedule'),
      cell: (l) => (
        <span title={l.scheduleId}>
          {scheduleOf(l) ? zoneName(scheduleOf(l).zoneId) : t('irrigation.journal.scheduleRef', { id: String(l.scheduleId || '').slice(0, 8) })}
        </span>
      ),
    },
    { key: 'actualStartTime', header: t('irrigation.journal.columns.start'), sortable: true, cell: (l) => instant(l.actualStartTime) },
    { key: 'actualEndTime', header: t('irrigation.journal.columns.stop'), cell: (l) => instant(l.actualEndTime) || t('common.none') },
    {
      key: 'duration',
      header: t('irrigation.journal.columns.duration'),
      cell: (l) =>
        l.actualEndTime
          ? format.minutes(Math.round((new Date(l.actualEndTime) - new Date(l.actualStartTime)) / 60000))
          : t('common.none'),
    },
    {
      key: 'waterUsedLiters',
      header: t('irrigation.journal.columns.volume'),
      sortable: true,
      cell: (l) => <strong className="font-semibold">{format.liters(l.waterUsedLiters ?? 0)}</strong>,
    },
    {
      key: 'status',
      header: t('irrigation.journal.columns.status'),
      cell: (l) => (
        <Badge tone={l.status === 'completed' ? 'success' : 'warning'}>
          {l.status === 'completed' ? t('irrigation.status.completed') : t('irrigation.status.running')}
        </Badge>
      ),
    },
  ]

  function renderSchedules() {
    if (loading) return <LoadingState label={t('irrigation.schedules.loading')} />
    if (error) return <ErrorState message={error.message || t('irrigation.schedules.loadError')} />
    if (activeSchedules.length === 0) {
      return <EmptyState title={t('irrigation.schedules.emptyTitle')} message={t('irrigation.schedules.empty')} />
    }
    return (
      <ul className="flex flex-col">
        {activeSchedules.map((s) => {
          const st = statusMeta(s.status)
          const status = String(s.status || '').toLowerCase()
          const canStop = ['running', 'in_progress', 'in-progress'].includes(status)
          const canStart = ['scheduled', 'postponed', 'saved'].includes(status)
          return (
            <li key={s.id} className="flex flex-wrap items-center gap-4 border-b border-border-subtle py-3 last:border-b-0">
              <p className="min-w-40 flex-1 text-sm font-semibold">
                {zoneName(s.zoneId)}
                <span className="mt-1 block text-xs font-medium text-text-muted">
                  {instant(s.startTime)}
                  {/* Plants block watered by the zone (only when the zone maps to a single block). */}
                  {s.blockCode && ` · ${t('irrigation.schedules.block', { code: s.blockCode })}`}
                  {s.multipleBlockCodes && ` · ${t('irrigation.schedules.severalBlocks')}`}
                </span>
              </p>
              <p className="min-w-28 text-sm">
                {format.minutes(s.durationMinutes ?? 0)}
                <span className="mt-1 block text-xs font-medium text-text-muted">
                  {t('irrigation.schedules.planned', { volume: format.liters(s.waterQuantityLiters ?? 0) })}
                </span>
              </p>
              <Badge tone={st.tone}>{t(`irrigation.status.${st.key}`)}</Badge>
              <div className="ml-auto flex gap-1">
                {canStart && <IconButton icon={Play} label={t('irrigation.schedules.start')} onClick={() => handleAction(s, 'start')} />}
                {canStop && <IconButton icon={Pause} label={t('irrigation.schedules.stop')} onClick={() => handleAction(s, 'stop')} />}
                <IconButton icon={Pencil} label={t('common.edit')} onClick={() => editSchedule(s)} />
              </div>
            </li>
          )
        })}
      </ul>
    )
  }

  function renderJournal() {
    if (logsLoading) return <LoadingState compact label={t('irrigation.journal.loading')} />
    if (completedLogs.length === 0) {
      return <EmptyState title={t('irrigation.journal.emptyTitle')} message={t('irrigation.journal.empty')} />
    }
    return <Table caption={t('irrigation.journal.title')} columns={journalColumns} rows={logList.items} {...tableSort(logList)} />
  }

  return (
    <Screen title={t('irrigation.title')} subtitle={t('irrigation.subtitle')}>
      <SplitLayout
        main={
          <>
            <Card
              title={t('irrigation.schedules.title')}
              actions={<span className="text-sm text-text-muted">{t('irrigation.schedules.count', { count: activeSchedules.length })}</span>}
            >
              {renderSchedules()}
              {schedules.length > activeSchedules.length && (
                <p className="mt-4 text-xs text-text-muted">{t('irrigation.schedules.archived')}</p>
              )}
            </Card>
            <Card
              title={t('irrigation.journal.title')}
              actions={<span className="text-sm text-text-muted">{t('irrigation.journal.count', { count: completedLogs.length })}</span>}
            >
              <Filters>
                <SearchField value={logList.query} onChange={logList.setQuery} placeholder={t('irrigation.journal.search')} />
              </Filters>
              {renderJournal()}
              <p className="mt-4 text-xs text-text-muted">
                {t('irrigation.journal.limit', { count: VISIBLE_LIMIT })}
                {completedLogs.length > VISIBLE_LIMIT ? ` ${t('irrigation.journal.olderStored')}` : ` ${t('irrigation.journal.moreStored')}`}
              </p>
            </Card>
          </>
        }
        aside={
          <Card title={editingScheduleId ? t('irrigation.form.editTitle') : t('irrigation.form.newTitle')}>
            <FormAlert>{formError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={submitSchedule}>
              <Field label={t('irrigation.form.zone')} required>
                <Select value={scheduleForm.zoneId} onChange={updateScheduleForm('zoneId')}>
                  <option value="">{t('common.select')}</option>
                  {zones.map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}
                </Select>
              </Field>
              <Field label={t('irrigation.form.start')} required>
                <Input type="datetime-local" value={scheduleForm.startTime} onChange={updateScheduleForm('startTime')} />
              </Field>
              <Field label={t('irrigation.form.duration')} required>
                <Input type="number" min="1" step="1" value={scheduleForm.durationMinutes} onChange={updateScheduleForm('durationMinutes')} />
              </Field>
              <Field label={t('irrigation.form.volume')} required>
                <Input type="number" min="0" step="0.01" value={scheduleForm.waterQuantityLiters} onChange={updateScheduleForm('waterQuantityLiters')} />
              </Field>
              <Field label={t('irrigation.form.status')}>
                <Select value={scheduleForm.status} onChange={updateScheduleForm('status')}>
                  <option value="scheduled">{t('irrigation.status.scheduled')}</option>
                  <option value="postponed">{t('irrigation.status.postponed')}</option>
                </Select>
              </Field>
              <FormActions>
                {editingScheduleId && <Button variant="secondary" onClick={resetScheduleForm}>{t('common.cancel')}</Button>}
                <Button type="submit" loading={busy}>{t('common.save')}</Button>
              </FormActions>
            </form>
          </Card>
        }
      />
    </Screen>
  )
}
