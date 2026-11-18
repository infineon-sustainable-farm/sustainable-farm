import { useRef, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, Field, IconButton, Input, Select, Table } from '../../../../shared/ui'
import { productTransformationApi } from '../../api/watersupplyApi'
import { useWaterFormat } from '../../format'
import { FormActions } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { Stat, StatGrid } from '../common/Stat'
import { ACTIVITIES, useActivityLabel } from './lotActivities'
import { fromLocalInput } from './datetime'

const BUFFER_TONES = { ok: 'success', tight: 'warning', insufficient: 'error' }

const EMPTY_BATCH = { lotId: '', activity: 'WASHING', sizeKg: '', plannedAt: '' }

/**
 * Availability forecast of a source for planned batches (Product Transformation): current level
 * minus the batches' demand and, by default, the scheduled irrigations of the source's farm. A tight
 * or insufficient buffer also raises an alert for the water team (backend side).
 */
export function AvailabilityForecastPanel({ sources }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const activityLabel = useActivityLabel()
  // Stable keys of the batch rows (removing a row must not shift the others' inputs).
  const nextKey = useRef(1)
  const [sourceId, setSourceId] = useState('')
  const [horizonDays, setHorizonDays] = useState('7')
  const [includeIrrigation, setIncludeIrrigation] = useState(true)
  const [batches, setBatches] = useState([{ ...EMPTY_BATCH, key: 0 }])
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  const updateBatch = (index, key) => (event) =>
    setBatches((current) => current.map((batch, i) => (i === index ? { ...batch, [key]: event.target.value } : batch)))

  const addBatch = () => {
    const key = nextKey.current
    nextKey.current += 1
    setBatches((current) => [...current, { ...EMPTY_BATCH, key }])
  }

  const submit = (event) => {
    event.preventDefault()
    if (!sourceId) {
      setError(t('lots.forecast.errors.sourceRequired'))
      return
    }
    const horizon = Number(horizonDays)
    if (!(horizon >= 1 && horizon <= 90)) {
      setError(t('lots.forecast.errors.horizonRange'))
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
        setError(err.message || t('lots.forecast.errors.failed'))
      })
      .finally(() => setBusy(false))
  }

  const rows = batches.map((batch, index) => ({ ...batch, index }))
  const columns = [
    {
      key: 'lotId',
      header: t('lots.forecast.columns.lot'),
      cell: (batch) => (
        <Input value={batch.lotId} onChange={updateBatch(batch.index, 'lotId')} aria-label={t('lots.forecast.batchLot', { number: batch.index + 1 })} />
      ),
    },
    {
      key: 'activity',
      header: t('lots.forecast.columns.activity'),
      cell: (batch) => (
        <Select value={batch.activity} onChange={updateBatch(batch.index, 'activity')} aria-label={t('lots.forecast.batchActivity', { number: batch.index + 1 })}>
          {ACTIVITIES.map(([key]) => <option key={key} value={key}>{activityLabel(key)}</option>)}
        </Select>
      ),
    },
    {
      key: 'sizeKg',
      header: t('lots.forecast.columns.size'),
      cell: (batch) => (
        <Input
          type="number"
          min="0"
          step="0.1"
          value={batch.sizeKg}
          onChange={updateBatch(batch.index, 'sizeKg')}
          aria-label={t('lots.forecast.batchSize', { number: batch.index + 1 })}
        />
      ),
    },
    {
      key: 'plannedAt',
      header: t('lots.forecast.columns.plannedAt'),
      cell: (batch) => (
        <Input
          type="datetime-local"
          value={batch.plannedAt}
          onChange={updateBatch(batch.index, 'plannedAt')}
          aria-label={t('lots.forecast.batchDate', { number: batch.index + 1 })}
        />
      ),
    },
  ]

  const statusTone = result ? BUFFER_TONES[result.bufferStatus] ?? 'info' : 'info'
  const statusText = result
    ? t(`lots.forecast.buffer.${result.bufferStatus}`, { defaultValue: result.bufferStatus })
    : ''

  return (
    <Card title={t('lots.forecast.title')} subtitle={t('lots.forecast.subtitle')}>
      <FormAlert>{error}</FormAlert>
      <form className="flex flex-col gap-4" onSubmit={submit}>
        <div className="grid items-end gap-4 md:grid-cols-3">
          <Field label={t('lots.forecast.fields.source')} required>
            <Select value={sourceId} onChange={(event) => setSourceId(event.target.value)}>
              <option value="">{t('common.select')}</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </Select>
          </Field>
          <Field label={t('lots.forecast.fields.horizon')} required>
            <Input type="number" min="1" max="90" value={horizonDays} onChange={(event) => setHorizonDays(event.target.value)} />
          </Field>
          <label className="flex min-h-10 items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="size-4 accent-primary"
              checked={includeIrrigation}
              onChange={(event) => setIncludeIrrigation(event.target.checked)}
            />
            {t('lots.forecast.fields.includeIrrigation')}
          </label>
        </div>

        <Table
          caption={t('lots.forecast.batchesCaption')}
          columns={columns}
          rows={rows}
          getRowKey={(batch) => batch.key}
          rowActions={(batch) => (
            <IconButton
              icon={Trash2}
              tone="danger"
              label={t('lots.forecast.removeBatch')}
              disabled={batches.length === 1}
              onClick={() => setBatches((current) => current.filter((_, i) => i !== batch.index))}
            />
          )}
        />

        <FormActions>
          <Button variant="secondary" icon={Plus} onClick={addBatch}>{t('lots.forecast.addBatch')}</Button>
          <Button type="submit" loading={busy}>{t('lots.forecast.submit')}</Button>
        </FormActions>
      </form>

      {result && (
        <div className="mt-4 flex flex-col gap-3 text-sm">
          <StatGrid>
            <Stat label={t('lots.forecast.result.current')} value={format.liters(result.currentLevelLiters)} />
            <Stat label={t('lots.forecast.result.batches')} value={format.liters(result.batchDemandLiters)} />
            <Stat label={t('lots.forecast.result.irrigations')} value={format.liters(result.irrigationDemandLiters)} />
            <Stat
              label={t('lots.forecast.result.left', { days: format.number(result.horizonDays) })}
              value={format.liters(result.availableAfterForecastLiters)}
            />
          </StatGrid>
          <p className="flex flex-wrap items-center gap-2">
            <Badge tone={statusTone}>{statusText}</Badge>
            {t('lots.forecast.result.buffer', { value: format.liters(result.bufferMinLiters) })}
          </p>
          {result.warnings.length > 0 && (
            <p className="text-xs text-warning-fg">
              {t('lots.forecast.result.noCoefficient', { activities: result.warnings.join(', ') })}
            </p>
          )}
          {result.parametersStatus === 'unvalidated_defaults' && (
            <p className="text-xs text-text-muted">{t('lots.forecast.result.defaults')}</p>
          )}
        </div>
      )}
    </Card>
  )
}
