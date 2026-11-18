import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Card, EmptyState, ErrorState, Select, Table } from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { useWaterQualityTests, useWaterSources } from '../../hooks/useWaterData'
import { useWaterFormat } from '../../format'
import { Filters, Screen, SplitLayout } from '../common/Screen'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { tableSort } from '../common/listSort'
import { sameId } from './ids'

const phOutOfRange = (ph) => ph != null && (ph < 6 || ph > 7.5)
const turbidityOutOfRange = (ntu) => ntu != null && ntu > 5

function isOutOfRange(test) {
  return phOutOfRange(test.ph) || turbidityOutOfRange(test.turbidityNtu)
}

/** Label and value of the reference thresholds panel. */
function Threshold({ label, children }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <dt className="text-text-muted">{label}</dt>
      <dd className="font-semibold">{children}</dd>
    </div>
  )
}

/**
 * Water quality - READ-ONLY view.
 * Measurements come from IoT probes (pH, turbidity, temperature, conductivity)
 * reported through the ingestion endpoint: no manual entry.
 * Thresholds: pH 6 - 7.5 and turbidity < 5 NTU -> alert + automatic notification.
 */
export function QualityView() {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { tests, loading, error } = useWaterQualityTests()
  const { sources } = useWaterSources()
  const [sourceFilter, setSourceFilter] = useState('')

  const sourceFiltered = useMemo(
    () => tests
      .filter((test) => !sourceFilter || sameId(test.sourceId, sourceFilter))
      .sort((a, b) => Number(isOutOfRange(b)) - Number(isOutOfRange(a))),
    [tests, sourceFilter],
  )

  const list = useListControls(sourceFiltered, { searchFields: ['testDate'] })
  const alertsCount = useMemo(() => sourceFiltered.filter(isOutOfRange).length, [sourceFiltered])

  const sourceName = (test) =>
    sources.find((source) => source.id === test.sourceId)?.name
    || (test.sourceId != null ? String(test.sourceId).slice(0, 8) : null)
    || t('quality.unknown')

  const columns = [
    { key: 'testDate', header: t('quality.columns.date'), sortable: true, cell: (test) => format.instant(test.testDate) },
    { key: 'source', header: t('quality.columns.source'), cell: sourceName },
    {
      key: 'ph',
      header: t('quality.columns.ph'),
      cell: (test) => (
        <strong className={phOutOfRange(test.ph) ? 'text-error-fg' : undefined}>
          {test.ph == null ? t('quality.unknown') : format.number(test.ph)}
        </strong>
      ),
    },
    {
      key: 'turbidity',
      header: t('quality.columns.turbidity'),
      cell: (test) => (
        <strong className={turbidityOutOfRange(test.turbidityNtu) ? 'text-warning-fg' : undefined}>
          {t('quality.ntu', { value: test.turbidityNtu == null ? t('quality.unknown') : format.number(test.turbidityNtu) })}
        </strong>
      ),
    },
    {
      key: 'status',
      header: t('quality.columns.status'),
      cell: (test) => (isOutOfRange(test)
        ? <Badge tone="error">{t('quality.alert')}</Badge>
        : <Badge tone="success">{t('quality.compliant')}</Badge>),
    },
  ]

  function renderList() {
    if (loading) return <LoadingState label={t('quality.loading')} />
    if (error) return <ErrorState message={error.message || t('quality.loadError')} />
    if (sourceFiltered.length === 0) return <EmptyState title={t('quality.emptyTitle')} message={t('quality.empty')} />
    return <Table caption={t('quality.listTitle')} columns={columns} rows={list.items} {...tableSort(list)} />
  }

  return (
    <Screen title={t('quality.title')} subtitle={t('quality.subtitle')}>
      <SplitLayout
        main={
          <Card
            title={t('quality.listTitle')}
            actions={<span className="text-sm text-text-muted">{t('quality.alertsCount', { count: alertsCount })}</span>}
          >
            <Filters>
              <Select aria-label={t('quality.sourceFilter')} value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)} className="w-auto">
                <option value="">{t('quality.allSources')}</option>
                {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
              </Select>
              <SearchField value={list.query} onChange={list.setQuery} placeholder={t('quality.search')} />
            </Filters>
            {renderList()}
            <ListPagination list={list} />
          </Card>
        }
        aside={
          <>
            <Card title={t('quality.thresholdsTitle')}>
              <dl className="flex flex-col gap-3 text-sm">
                <Threshold label={t('quality.compliantPh')}>{t('quality.phRange')}</Threshold>
                <Threshold label={t('quality.maxTurbidity')}>{t('quality.ntu', { value: format.number(5) })}</Threshold>
                <Threshold label={t('quality.outOfRange')}><Badge tone="error">{t('quality.outOfRangeAction')}</Badge></Threshold>
              </dl>
            </Card>
            <Card title={t('quality.dataSourceTitle')}>
              <p className="text-sm text-text-muted">{t('quality.dataSourceText')}</p>
            </Card>
          </>
        }
      />
    </Screen>
  )
}
