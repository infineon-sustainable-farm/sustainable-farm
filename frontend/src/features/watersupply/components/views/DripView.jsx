import { useMemo, useState } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Field,
  IconButton,
  Input,
  Select,
  Table,
  Textarea,
} from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { dripMaintenanceApi } from '../../api/watersupplyApi'
import { useDripMaintenanceLogs } from '../../hooks/useWaterData'
import { useZones } from '../../hooks/useFarms'
import { useWaterFormat } from '../../format'
import { Filters, FormActions, Screen, SplitLayout } from '../common/Screen'
import { ChoiceChips } from '../common/ChoiceChips'
import { FormAlert } from '../common/FormAlert'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { tableSort } from '../common/listSort'
import { sameId } from './ids'

const DAY_MS = 24 * 3600 * 1000
const MAINTENANCE_TYPES = ['cleaning', 'filter_change', 'repair', 'inspection']
const PERIODS = ['week', 'month', 'all']
const SEVERITIES = ['low', 'medium', 'high']
const EMPTY_FORM = {
  zoneId: '',
  maintenanceType: 'cleaning',
  maintenanceDate: '',
  filterCleaned: false,
  cloggingDetected: false,
  cloggingSeverity: '',
  emitterReplacedCount: '',
  notes: '',
  performedBy: '',
}
// Native checkbox (the kit has none), in the primary colour with the kit focus mark.
const CHECKBOX = 'size-4 accent-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring'

function severityTone(severity) {
  switch ((severity || '').toLowerCase()) {
    case 'high': return 'error'
    case 'medium': return 'warning'
    default: return 'success'
  }
}

/**
 * Drip view: maintenance interventions (full CRUD).
 * Backend contract: DripMaintenanceLog { zoneId, maintenanceDate, maintenanceType,
 * filterCleaned, cloggingDetected, cloggingSeverity, emitterReplacedCount, notes, performedBy }.
 */
export function DripView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { logs, loading, error, refetch } = useDripMaintenanceLogs()
  const { zones } = useZones()
  const [period, setPeriod] = useState('all')
  const [zoneFilter, setZoneFilter] = useState('')
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)
  const [deleteError, setDeleteError] = useState(null)

  // Filter based on the max data date (pure function - no Date.now() during render).
  const maxDateMs = useMemo(() => {
    let max = 0
    for (const item of logs) {
      const time = new Date(item.maintenanceDate || item.createdAt).getTime()
      if (!Number.isNaN(time) && time > max) max = time
    }
    return max
  }, [logs])

  const zoneFiltered = useMemo(() => {
    let list = logs.filter((item) => !zoneFilter || sameId(item.zoneId, zoneFilter))
    if (period !== 'all') {
      const days = period === 'week' ? 7 : 30
      const cutoff = maxDateMs - days * DAY_MS
      list = list.filter((item) => new Date(item.maintenanceDate || item.createdAt).getTime() >= cutoff)
    }
    return list
  }, [logs, zoneFilter, period, maxDateMs])

  // Client-side search + sort + pagination (sort by date desc by default).
  const list = useListControls(zoneFiltered, {
    searchFields: ['maintenanceType', 'notes', 'performedBy'],
    defaultSort: { key: 'maintenanceDate', dir: 'desc' },
  })

  const zoneName = (zoneId) =>
    zones.find((z) => z.id === zoneId)?.name || (zoneId ? t('maintenance.zoneFallback', { id: String(zoneId).slice(0, 8) }) : t('common.none'))
  const typeLabel = (type) => (type ? t(`maintenance.types.${type}`, { defaultValue: type }) : t('common.none'))

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
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  const submitLog = (event) => {
    event.preventDefault()
    if (!form.zoneId) {
      setFormError(t('maintenance.errors.zoneRequired'))
      return
    }
    if (!form.maintenanceDate) {
      setFormError(t('maintenance.errors.dateRequired'))
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
        notify(editingId ? t('maintenance.updated') : t('maintenance.saved'))
        resetForm()
      })
      .catch((err) => setFormError(err.message || t('common.saveError')))
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
    setDeleteError(null)
    dripMaintenanceApi
      .deleteLog(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify(t('maintenance.deleted'))
        setDeleteTarget(null)
      })
      .catch((err) => setDeleteError(err.message || t('common.deleteError')))
      .finally(() => setBusy(false))
  }

  const recentCount = logs.filter(
    (item) => new Date(item.maintenanceDate || item.createdAt).getTime() >= maxDateMs - 7 * DAY_MS,
  ).length

  const cloggingLabel = (severity) =>
    t('maintenance.clogging', {
      severity: severity ? t(`maintenance.severityInline.${severity}`, { defaultValue: severity }) : '',
    }).trim()

  const columns = [
    {
      key: 'maintenanceDate',
      header: t('maintenance.columns.date'),
      sortable: true,
      cell: (item) => (item.maintenanceDate ? format.instant(item.maintenanceDate) : ''),
    },
    { key: 'zone', header: t('maintenance.columns.zone'), cell: (item) => zoneName(item.zoneId) },
    { key: 'maintenanceType', header: t('maintenance.columns.type'), sortable: true, cell: (item) => typeLabel(item.maintenanceType) },
    {
      key: 'state',
      header: t('maintenance.columns.state'),
      cell: (item) =>
        item.cloggingDetected ? (
          <Badge tone={severityTone(item.cloggingSeverity)}>{cloggingLabel(item.cloggingSeverity)}</Badge>
        ) : (
          <Badge tone="success">{t('maintenance.ok')}</Badge>
        ),
    },
    { key: 'emitters', header: t('maintenance.columns.emitters'), cell: (item) => format.number(item.emitterReplacedCount ?? 0) },
  ]

  function renderList() {
    if (loading) return <LoadingState label={t('maintenance.loading')} />
    if (error) return <ErrorState message={error.message || t('maintenance.loadError')} />
    if (list.items.length === 0) return <EmptyState title={t('maintenance.emptyTitle')} message={t('maintenance.empty')} />
    return (
      <>
        <Table
          caption={t('maintenance.historyTitle')}
          columns={columns}
          rows={list.items}
          {...tableSort(list)}
          rowActions={(item) => (
            <div className="flex justify-end gap-1">
              <IconButton icon={Pencil} label={t('common.edit')} onClick={() => editLog(item)} />
              <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => setDeleteTarget(item)} />
            </div>
          )}
        />
        <ListPagination list={list} />
      </>
    )
  }

  return (
    <Screen title={t('maintenance.title')} subtitle={t('maintenance.subtitle')}>
      <SplitLayout
        main={
          <>
            <Card
              title={t('maintenance.historyTitle')}
              actions={
                <ChoiceChips
                  label={t('maintenance.periodLabel')}
                  options={PERIODS.map((value) => ({ value, label: t(`maintenance.periods.${value}`) }))}
                  value={period}
                  onChange={setPeriod}
                />
              }
            >
              <Filters>
                <Select
                  aria-label={t('maintenance.zoneFilter')}
                  value={zoneFilter}
                  onChange={(event) => setZoneFilter(event.target.value)}
                  className="w-auto"
                >
                  <option value="">{t('maintenance.allZones')}</option>
                  {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
                </Select>
                <SearchField value={list.query} onChange={list.setQuery} placeholder={t('maintenance.search')} />
              </Filters>
              {renderList()}
            </Card>
            <Card title={t('maintenance.planningTitle')}>
              <p className="text-sm text-text-muted">
                {t('maintenance.planning')} {t('maintenance.recent', { count: recentCount })}
              </p>
            </Card>
          </>
        }
        aside={
          <Card title={editingId ? t('maintenance.form.editTitle') : t('maintenance.form.newTitle')}>
            <FormAlert>{formError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={submitLog}>
              <Field label={t('maintenance.form.zone')} required>
                <Select value={form.zoneId} onChange={updateForm('zoneId')}>
                  <option value="">{t('common.select')}</option>
                  {zones.map((zone) => <option key={zone.id} value={zone.id}>{zone.name}</option>)}
                </Select>
              </Field>
              <Field label={t('maintenance.form.type')} required>
                <Select value={form.maintenanceType} onChange={updateForm('maintenanceType')}>
                  {MAINTENANCE_TYPES.map((type) => <option key={type} value={type}>{typeLabel(type)}</option>)}
                </Select>
              </Field>
              <Field label={t('maintenance.form.date')} required>
                <Input type="datetime-local" value={form.maintenanceDate} onChange={updateForm('maintenanceDate')} />
              </Field>
              <label className="flex items-center gap-2 text-sm font-medium">
                <input type="checkbox" className={CHECKBOX} checked={form.filterCleaned} onChange={updateForm('filterCleaned')} />
                {t('maintenance.form.filterCleaned')}
              </label>
              <label className="flex items-center gap-2 text-sm font-medium">
                <input type="checkbox" className={CHECKBOX} checked={form.cloggingDetected} onChange={updateForm('cloggingDetected')} />
                {t('maintenance.form.cloggingDetected')}
              </label>
              {form.cloggingDetected && (
                <Field label={t('maintenance.form.severity')}>
                  <Select value={form.cloggingSeverity} onChange={updateForm('cloggingSeverity')}>
                    <option value="">{t('common.select')}</option>
                    {SEVERITIES.map((severity) => (
                      <option key={severity} value={severity}>{t(`maintenance.severities.${severity}`)}</option>
                    ))}
                  </Select>
                </Field>
              )}
              <Field label={t('maintenance.form.emitters')}>
                <Input type="number" min="0" step="1" value={form.emitterReplacedCount} onChange={updateForm('emitterReplacedCount')} />
              </Field>
              <Field label={t('maintenance.form.performedBy')}>
                <Input value={form.performedBy} onChange={updateForm('performedBy')} />
              </Field>
              <Field label={t('maintenance.form.notes')}>
                <Textarea value={form.notes} onChange={updateForm('notes')} />
              </Field>
              <FormActions>
                {editingId && <Button variant="secondary" onClick={resetForm}>{t('common.cancel')}</Button>}
                <Button type="submit" loading={busy}>{t('common.save')}</Button>
              </FormActions>
            </form>
          </Card>
        }
      />
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('maintenance.deleteTitle')}
        message={t('maintenance.deleteMessage')}
        onConfirm={deleteLog}
        onClose={() => {
          setDeleteTarget(null)
          setDeleteError(null)
        }}
        loading={busy}
        error={deleteError}
      />
    </Screen>
  )
}
