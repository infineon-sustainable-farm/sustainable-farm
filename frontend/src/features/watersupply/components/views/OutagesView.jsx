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
} from '../../../../shared/ui'
import { outageApi } from '../../api/watersupplyApi'
import { useFarms, useZones } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useOutages } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { ChoiceChips } from '../common/ChoiceChips'
import { FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { Stat, StatGrid } from '../common/Stat'
import { fromLocalInput, toLocalInput } from './datetime'

const SCOPES = ['zone', 'source', 'farm']
const FILTERS = ['current', 'past', 'cancelled', 'all']
const STATUSES = ['planned', 'active', 'cancelled']

const EMPTY_FORM = { scope: 'zone', scopeId: '', startAt: '', endAt: '', reason: '', status: 'planned', postponeIrrigations: true }

/** In force now / upcoming / finished or cancelled: computed on the client from the period. */
function phaseOf(outage, now) {
  if (outage.status === 'cancelled') return { key: 'cancelled', tone: 'error' }
  const start = new Date(outage.startAt).getTime()
  const end = new Date(outage.endAt).getTime()
  if (start <= now && now < end) return { key: 'active', tone: 'warning' }
  if (start > now) return { key: 'upcoming', tone: 'info' }
  return { key: 'past', tone: 'success' }
}

/**
 * Planned water unavailability (outage or maintenance) on a zone, a source or a whole farm.
 *
 * These periods are read by Visitor Management, refuse valve openings and skip the automatic
 * irrigation; a farm-wide period applies to every zone of the farm. Backend contract:
 * WaterUnavailability { scope, scopeId, startAt, endAt, reason, status } (+ postponeIrrigations
 * on write, to postpone the irrigations planned during the period).
 */
export function OutagesView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { outages, loading, error, refetch } = useOutages()
  const { farms } = useFarms()
  const { sources } = useWaterSources()
  const { zones } = useZones()
  const [filter, setFilter] = useState('current')
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const targetsByScope = { zone: zones, source: sources, farm: farms }
  const scopeLabel = (scope) => t(`outages.scopes.${scope}`, { defaultValue: scope })
  const targetName = (scope, scopeId) =>
    (targetsByScope[scope] || []).find((item) => item.id === scopeId)?.name || `${scope} ${String(scopeId).slice(0, 8)}`

  // Reference instant of the screen, taken once: every phase of the list is computed against it.
  const [now] = useState(() => Date.now())
  const visible = useMemo(() => outages
    .map((outage) => ({ ...outage, phase: phaseOf(outage, now) }))
    .filter((outage) => filter === 'all' || (filter === 'current'
      ? ['active', 'upcoming'].includes(outage.phase.key)
      : outage.phase.key === filter))
    .sort((a, b) => String(a.startAt).localeCompare(String(b.startAt))), [outages, filter, now])
  const activeCount = outages.filter((outage) => phaseOf(outage, now).key === 'active').length

  const updateForm = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    // Changing the scope invalidates the selected target (a zone id is not a farm id).
    setForm((current) => ({ ...current, [key]: value, ...(key === 'scope' ? { scopeId: '' } : {}) }))
  }

  const resetForm = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setFormError(null)
  }

  const payloadOf = (values) => ({
    scope: values.scope,
    scopeId: values.scopeId,
    startAt: fromLocalInput(values.startAt),
    endAt: fromLocalInput(values.endAt),
    reason: values.reason.trim() || null,
    status: values.status,
    postponeIrrigations: values.scope !== 'source' && values.postponeIrrigations,
  })

  const submit = (event) => {
    event.preventDefault()
    if (!form.scopeId) {
      setFormError(t('outages.errors.targetRequired'))
      return
    }
    if (!form.startAt || !form.endAt) {
      setFormError(t('outages.errors.periodRequired'))
      return
    }
    if (new Date(form.endAt) <= new Date(form.startAt)) {
      setFormError(t('outages.errors.endAfterStart'))
      return
    }
    setBusy(true)
    setFormError(null)
    const payload = payloadOf(form)
    const request = editingId ? outageApi.updateOutage(editingId, payload) : outageApi.createOutage(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingId ? t('outages.updated') : t('outages.planned'))
        resetForm()
      })
      // The backend refuses an overlap on the same target: its message is shown as is.
      .catch((err) => setFormError(err.message || t('common.saveError')))
      .finally(() => setBusy(false))
  }

  const edit = (outage) => {
    setEditingId(outage.id)
    setForm({
      scope: outage.scope,
      scopeId: outage.scopeId,
      startAt: toLocalInput(outage.startAt),
      endAt: toLocalInput(outage.endAt),
      reason: outage.reason || '',
      status: outage.status || 'planned',
      postponeIrrigations: false,
    })
    setFormError(null)
  }

  const cancel = (outage) => {
    const values = { scope: outage.scope, scopeId: outage.scopeId, startAt: outage.startAt, endAt: outage.endAt, reason: outage.reason || '' }
    outageApi
      .updateOutage(outage.id, { ...values, status: 'cancelled', postponeIrrigations: false })
      .then(() => refetch())
      .then(() => notify(t('outages.cancelled')))
      .catch((err) => notify(err.message || t('outages.cancelError'), 'error'))
  }

  const remove = () => {
    if (!deleteTarget) return
    setBusy(true)
    outageApi
      .deleteOutage(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify(t('outages.deleted'))
        setDeleteTarget(null)
      })
      .catch((err) => notify(err.message || t('common.deleteError'), 'error'))
      .finally(() => setBusy(false))
  }

  const columns = [
    {
      key: 'target',
      header: t('outages.columns.what'),
      cell: (outage) => (
        <>
          {targetName(outage.scope, outage.scopeId)}
          <p className="text-xs font-normal text-text-muted">{scopeLabel(outage.scope)}</p>
        </>
      ),
    },
    { key: 'startAt', header: t('outages.columns.from'), cell: (outage) => format.instant(outage.startAt) },
    { key: 'endAt', header: t('outages.columns.to'), cell: (outage) => format.instant(outage.endAt) },
    { key: 'reason', header: t('outages.columns.reason'), cell: (outage) => outage.reason || t('common.none') },
    {
      key: 'status',
      header: t('outages.columns.status'),
      cell: (outage) => <Badge tone={outage.phase.tone}>{t(`outages.phases.${outage.phase.key}`)}</Badge>,
    },
  ]

  function renderList() {
    if (loading) return <LoadingState label={t('outages.loading')} />
    if (error) return <ErrorState message={error.message || t('outages.loadError')} />
    if (visible.length === 0) return <EmptyState title={t('outages.emptyTitle')} message={t('outages.empty')} />
    return (
      <Table
        caption={t('outages.listTitle')}
        columns={columns}
        rows={visible}
        rowActions={(outage) => (
          <div className="flex items-center justify-end gap-1">
            <IconButton icon={Pencil} label={t('common.edit')} onClick={() => edit(outage)} />
            {outage.status !== 'cancelled' && outage.phase.key !== 'past' && (
              <Button size="sm" variant="secondary" onClick={() => cancel(outage)}>{t('common.cancel')}</Button>
            )}
            <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => setDeleteTarget(outage)} />
          </div>
        )}
      />
    )
  }

  return (
    <Screen title={t('outages.title')} subtitle={t('outages.subtitle')}>
      <StatGrid>
        <Stat
          label={t('outages.inForceNow')}
          value={<span className={activeCount ? 'text-warning-fg' : undefined}>{activeCount}</span>}
        />
        <Stat label={t('outages.plannedPeriods')} value={outages.filter((outage) => outage.status !== 'cancelled').length} />
      </StatGrid>

      <SplitLayout
        main={
          <Card
            title={t('outages.listTitle')}
            actions={
              <ChoiceChips
                label={t('outages.filterLabel')}
                options={FILTERS.map((key) => ({ value: key, label: t(`outages.filters.${key}`) }))}
                value={filter}
                onChange={setFilter}
              />
            }
          >
            {renderList()}
          </Card>
        }
        aside={
          <Card title={editingId ? t('outages.editTitle') : t('outages.newTitle')}>
            <FormAlert>{formError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={submit}>
              <Field label={t('outages.fields.scope')} required>
                <Select value={form.scope} onChange={updateForm('scope')}>
                  {SCOPES.map((scope) => <option key={scope} value={scope}>{scopeLabel(scope)}</option>)}
                </Select>
              </Field>
              <Field label={scopeLabel(form.scope)} required>
                <Select value={form.scopeId} onChange={updateForm('scopeId')}>
                  <option value="">{t('common.select')}</option>
                  {(targetsByScope[form.scope] || []).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </Select>
              </Field>
              <Field label={t('outages.fields.start')} required>
                <Input type="datetime-local" value={form.startAt} onChange={updateForm('startAt')} />
              </Field>
              <Field label={t('outages.fields.end')} required>
                <Input type="datetime-local" value={form.endAt} onChange={updateForm('endAt')} />
              </Field>
              <Field label={t('outages.fields.reason')}>
                <Input value={form.reason} onChange={updateForm('reason')} placeholder={t('outages.fields.reasonPlaceholder')} />
              </Field>
              {editingId && (
                <Field label={t('outages.fields.status')}>
                  <Select value={form.status} onChange={updateForm('status')}>
                    {STATUSES.map((status) => <option key={status} value={status}>{t(`outages.statuses.${status}`)}</option>)}
                  </Select>
                </Field>
              )}
              {form.scope === 'source' ? (
                <p className="text-xs text-text-muted">{t('outages.sourceNoPostpone')}</p>
              ) : (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    className="size-4 accent-primary"
                    checked={form.postponeIrrigations}
                    onChange={updateForm('postponeIrrigations')}
                  />
                  {t('outages.fields.postpone')}
                </label>
              )}
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
        title={t('outages.deleteTitle')}
        message={t('outages.deleteMessage')}
        onConfirm={remove}
        onClose={() => setDeleteTarget(null)}
        loading={busy}
      />
    </Screen>
  )
}
