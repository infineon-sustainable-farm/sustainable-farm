import { useState } from 'react'
import { Search } from 'lucide-react'
import { Trans, useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, ErrorState, Field, Input, Select, Table } from '../../../../shared/ui'
import { productTransformationApi } from '../../api/watersupplyApi'
import { useWaterSources } from '../../hooks/useWaterData'
import { useLotCompliance, useLotConsumption } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { FormActions, Screen, SplitLayout } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { Stat } from '../common/Stat'
import { AvailabilityForecastPanel } from './AvailabilityForecastPanel'
import { ACTIVITIES, useActivityLabel } from './lotActivities'
import { fromLocalInput } from './datetime'

const COMPLIANCE_TONES = { compliant: 'success', non_compliant: 'error', no_test: 'warning' }
const USAGE_WARNINGS = ['no_consumption_found', 'window_overlaps_other_lot', 'no_source_or_zone_filter']

const EMPTY_USAGE = { lotId: '', activity: 'WASHING', sourceId: '', startAt: '', endAt: '' }

/**
 * Processing lots (Product Transformation integration): water used per lot and activity, quality
 * of that water, attribution of a metering window to a lot, and the availability forecast.
 *
 * Meters know nothing about lots: Product Transformation (or the water team here) tells which
 * time window belongs to which lot, and the consumptions of that window are attributed to it.
 */
export function LotsView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const activityLabel = useActivityLabel()
  const { sources } = useWaterSources()
  const [lotQuery, setLotQuery] = useState('')
  const [activeLot, setActiveLot] = useState('')
  const [usage, setUsage] = useState(EMPTY_USAGE)
  const [usageResult, setUsageResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [usageError, setUsageError] = useState(null)

  const { data: lot, loading: lotLoading, error: lotError, refetch: refetchLot } = useLotConsumption(activeLot)
  const { data: compliance, loading: complianceLoading, error: complianceError, refetch: refetchCompliance } = useLotCompliance(activeLot)

  const sourceName = (sourceId) =>
    sources.find((source) => source.id === sourceId)?.name ||
    (sourceId ? t('lots.sourceFallback', { id: String(sourceId).slice(0, 8) }) : t('common.none'))
  const updateUsage = (key) => (event) => setUsage((current) => ({ ...current, [key]: event.target.value }))

  const lookUp = (event) => {
    event.preventDefault()
    setActiveLot(lotQuery.trim())
  }

  const attach = (event) => {
    event.preventDefault()
    if (!usage.lotId.trim()) {
      setUsageError(t('lots.errors.lotRequired'))
      return
    }
    // The API accepts a window without source, but it then sweeps every meter of the site.
    if (!usage.sourceId) {
      setUsageError(t('lots.errors.sourceRequired'))
      return
    }
    if (!usage.startAt || !usage.endAt || new Date(usage.endAt) <= new Date(usage.startAt)) {
      setUsageError(t('lots.errors.window'))
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
        notify(t('lots.attached', { count: result.attachedCount, lotId }))
        // Show the lot that was just updated.
        setLotQuery(lotId)
        if (lotId === activeLot) {
          refetchLot()
          refetchCompliance()
        } else {
          setActiveLot(lotId)
        }
      })
      .catch((err) => setUsageError(err.message || t('lots.errors.attachFailed')))
      .finally(() => setBusy(false))
  }

  const columns = [
    { key: 'date', header: t('lots.columns.date'), cell: (item) => format.instant(item.consumptionDate) },
    { key: 'activity', header: t('lots.columns.activity'), cell: (item) => activityLabel(item.activity) },
    { key: 'source', header: t('lots.columns.source'), cell: (item) => sourceName(item.sourceId) },
    { key: 'volume', header: t('lots.columns.volume'), cell: (item) => <strong>{format.liters(item.liters)}</strong> },
  ]

  function renderVerdict() {
    if (complianceLoading) return t('lots.verdictLoading')
    if (!compliance || !COMPLIANCE_TONES[compliance.status]) return t('common.none')
    return <Badge tone={COMPLIANCE_TONES[compliance.status]}>{t(`lots.compliance.${compliance.status}`)}</Badge>
  }

  function renderLot() {
    if (!activeLot) return <EmptyState title={t('lots.noLotTitle')} message={t('lots.noLot')} />
    if (lotLoading) return <LoadingState label={t('lots.loading')} />
    if (lotError) return <ErrorState message={lotError.message || t('lots.loadError')} />
    return (
      <div className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Stat label={t('lots.waterUsed', { lotId: lot.lotId })} value={format.liters(lot.totalLiters)} />
          <Stat label={t('lots.quality')} value={renderVerdict()} />
        </div>
        {complianceError && (
          <p className="text-xs text-text-muted">{t('lots.noVerdict', { message: complianceError.message })}</p>
        )}
        {compliance && (
          <p className="text-xs text-text-muted">
            {t('lots.complianceDetail', {
              tests: format.number(compliance.tests.length),
              from: format.instant(compliance.from),
              to: format.instant(compliance.to),
              phMin: format.number(compliance.rule.phMin),
              phMax: format.number(compliance.rule.phMax),
              turbidity: format.number(compliance.rule.turbidityMaxNtu),
            })}
            {compliance.status === 'no_test' && ` ${t('lots.noTestWarning')}`}
          </p>
        )}
        {lot.items.length === 0 ? (
          <EmptyState title={t('lots.emptyTitle')} message={t('lots.empty')} />
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              {Object.entries(lot.byActivity).map(([activity, liters]) => (
                <Badge key={activity} tone="info">
                  {t('lots.byActivity', { activity: activityLabel(activity), volume: format.liters(liters) })}
                </Badge>
              ))}
            </div>
            <Table caption={t('lots.consumptionsCaption')} columns={columns} rows={lot.items} getRowKey={(item) => item.consumptionId} />
          </>
        )}
      </div>
    )
  }

  return (
    <Screen title={t('lots.title')} subtitle={t('lots.subtitle')}>
      <SplitLayout
        main={
          <Card title={t('lots.lookupTitle')}>
            <form className="mb-4 flex flex-wrap items-center gap-3" onSubmit={lookUp}>
              <Input
                className="max-w-xs"
                value={lotQuery}
                onChange={(event) => setLotQuery(event.target.value)}
                placeholder={t('lots.lookupPlaceholder')}
                aria-label={t('lots.lookupLabel')}
              />
              <Button type="submit" icon={Search} disabled={!lotQuery.trim()}>{t('lots.lookup')}</Button>
            </form>
            {renderLot()}
          </Card>
        }
        aside={
          <Card title={t('lots.attachTitle')}>
            <FormAlert>{usageError}</FormAlert>
            <form className="flex flex-col gap-4" onSubmit={attach}>
              <Field label={t('lots.fields.lot')} required>
                <Input value={usage.lotId} onChange={updateUsage('lotId')} placeholder={t('lots.fields.lotPlaceholder')} />
              </Field>
              <Field label={t('lots.fields.activity')} required>
                <Select value={usage.activity} onChange={updateUsage('activity')}>
                  {ACTIVITIES.map(([key]) => <option key={key} value={key}>{activityLabel(key)}</option>)}
                </Select>
              </Field>
              <Field label={t('lots.fields.source')} required>
                <Select value={usage.sourceId} onChange={updateUsage('sourceId')}>
                  <option value="">{t('common.select')}</option>
                  {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
                </Select>
              </Field>
              <Field label={t('lots.fields.from')} required>
                <Input type="datetime-local" value={usage.startAt} onChange={updateUsage('startAt')} />
              </Field>
              <Field label={t('lots.fields.to')} required>
                <Input type="datetime-local" value={usage.endAt} onChange={updateUsage('endAt')} />
              </Field>
              <FormActions>
                <Button type="submit" loading={busy}>{t('lots.attach')}</Button>
              </FormActions>
            </form>
            {usageResult && (
              <div className="mt-4 text-sm">
                <Trans
                  t={t}
                  i18nKey="lots.attachedSummary"
                  values={{
                    count: format.number(usageResult.attachedCount),
                    volume: format.liters(usageResult.attachedLiters),
                    lotId: usageResult.lotId,
                  }}
                  components={{ strong: <strong /> }}
                />
                {usageResult.warnings.map((warning) => (
                  <p key={warning} className="mt-2 text-warning-fg">
                    {USAGE_WARNINGS.includes(warning) ? t(`lots.warnings.${warning}`) : warning}
                  </p>
                ))}
              </div>
            )}
          </Card>
        }
      />
      <AvailabilityForecastPanel sources={sources} />
    </Screen>
  )
}
