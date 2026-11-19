import { useMemo, useState } from 'react'
import { Download } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, ErrorState, Select, Table } from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { useFarms } from '../../hooks/useFarms'
import { useWaterConsumptions, useWaterSources } from '../../hooks/useWaterData'
import { downloadCsv } from '../../api/watersupplyApi'
import { useWaterFormat } from '../../format'
import { WsAreaChart } from '../charts'
import { C } from '../chartUtils'
import { Filters, Screen, SplitLayout } from '../common/Screen'
import { ChoiceChips } from '../common/ChoiceChips'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { Stat } from '../common/Stat'
import { tableSort } from '../common/listSort'
import { QuotaPanel } from './QuotaPanel'
import { activityLabel } from './lotActivities'

const PERIODS = ['week', 'month', 'all']

function groupDailySeries(consumptions) {
  const map = new Map()
  for (const item of consumptions) {
    const day = (item.consumptionDate || item.createdAt || '').slice(0, 10) || 'unknown'
    map.set(day, (map.get(day) || 0) + (item.consumptionLiters || 0))
  }
  return [...map.entries()]
    .sort(([a], [b]) => (a < b ? -1 : 1))
    .map(([day, liters]) => ({ day, liters: Math.round(liters) }))
}

/**
 * Consumption tracking, READ-ONLY view.
 * Volumes are measured by IoT sensors and reported automatically:
 * no manual entry (no create/edit/delete in the UI).
 */
export function ConsumptionView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { consumptions, loading, error } = useWaterConsumptions()
  const { farms } = useFarms()
  const { sources } = useWaterSources()
  const [period, setPeriod] = useState('all')
  const [farmFilter, setFarmFilter] = useState('')
  const [sourceFilter, setSourceFilter] = useState('')
  const [exporting, setExporting] = useState(false)

  // Server-side export of the consumption report (CSV, current period), P10.
  const exportReport = () => {
    setExporting(true)
    downloadCsv('consumption', period === 'all' ? 'month' : period)
      .then(() => notify?.(t('consumption.exported')))
      .catch((err) => notify?.(err.message || t('consumption.exportFailed'), 'error'))
      .finally(() => setExporting(false))
  }

  const filteredConsumptions = useMemo(() => consumptions.filter((item) => {
    if (farmFilter && item.farmId !== farmFilter) return false
    if (sourceFilter && item.sourceId !== sourceFilter) return false
    return true
  }), [consumptions, farmFilter, sourceFilter])

  // Search + sort + pagination for the table (the chart stays on the filtered data).
  const tableList = useListControls(filteredConsumptions, {
    searchFields: ['consumptionDate', 'createdAt', 'lotId', 'blockCode'],
    defaultSort: { key: 'consumptionDate', dir: 'desc' },
  })

  const maxDayMs = useMemo(() => {
    let max = 0
    for (const item of filteredConsumptions) {
      const time = new Date(item.consumptionDate || item.createdAt).getTime()
      if (!Number.isNaN(time) && time > max) max = time
    }
    return max
  }, [filteredConsumptions])

  const series = useMemo(() => {
    let list = filteredConsumptions
    if (period === 'month') {
      const cutoff = maxDayMs - 30 * 24 * 3600 * 1000
      list = filteredConsumptions.filter((item) => new Date(item.consumptionDate || item.createdAt).getTime() >= cutoff)
    } else if (period === 'week') {
      const cutoff = maxDayMs - 7 * 24 * 3600 * 1000
      list = filteredConsumptions.filter((item) => new Date(item.consumptionDate || item.createdAt).getTime() >= cutoff)
    }
    return groupDailySeries(list)
  }, [filteredConsumptions, period, maxDayMs])

  const totalLiters = useMemo(() => series.reduce((sum, item) => sum + item.liters, 0), [series])
  const peakDay = useMemo(() => series.reduce((max, item) => (item.liters > (max?.liters ?? -1) ? item : max), null), [series])

  const lotLabel = (item) =>
    item.lotId
      ? t('consumption.lotValue', {
          lot: item.lotId,
          activity: t(`lots.activities.${item.activity || 'OTHER'}`, { defaultValue: activityLabel(item.activity) }),
        })
      : t('common.none')

  const columns = [
    {
      key: 'consumptionDate',
      header: t('consumption.columns.date'),
      sortable: true,
      cell: (item) => (item.consumptionDate || item.createdAt || '').slice(0, 10),
    },
    {
      key: 'volume',
      header: t('consumption.columns.volume'),
      cell: (item) => <strong className="font-semibold">{format.liters(item.consumptionLiters ?? 0)}</strong>,
    },
    // A zone over several blocks cannot attribute its volume to one of them.
    {
      key: 'block',
      header: t('consumption.columns.block'),
      cell: (item) => item.blockCode || (item.multipleBlockCodes ? t('consumption.severalBlocks') : t('common.none')),
    },
    { key: 'lot', header: t('consumption.columns.lot'), cell: lotLabel },
  ]

  function renderChart() {
    if (loading) return <LoadingState compact label={t('consumption.chart.loading')} />
    if (error) return <ErrorState message={error.message || t('consumption.chart.loadError')} />
    if (series.length === 0) return <EmptyState title={t('consumption.chart.emptyTitle')} message={t('consumption.chart.empty')} />
    return (
      <WsAreaChart
        data={series}
        xKey="day"
        series={[{ key: 'liters', name: t('consumption.chart.series'), color: C.primary }]}
        exportName="water-consumption"
      />
    )
  }

  function renderTable() {
    if (loading) return <LoadingState compact label={t('consumption.history.loading')} />
    if (filteredConsumptions.length === 0) {
      return <EmptyState title={t('consumption.history.emptyTitle')} message={t('consumption.history.empty')} />
    }
    return (
      <>
        <Table caption={t('consumption.history.title')} columns={columns} rows={tableList.items} {...tableSort(tableList)} />
        <ListPagination list={tableList} />
      </>
    )
  }

  return (
    <Screen title={t('consumption.title')} subtitle={t('consumption.subtitle')}>
      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label={t('consumption.stats.period')} value={t(`consumption.periodNames.${period}`)} />
        <Stat label={t('consumption.stats.total')} value={format.liters(totalLiters)} />
        <Stat
          label={t('consumption.stats.peak')}
          value={peakDay ? t('consumption.stats.peakValue', { day: peakDay.day.slice(5), volume: format.liters(peakDay.liters) }) : t('consumption.stats.noPeak')}
        />
      </div>

      <SplitLayout
        main={
          <Card
            title={t('consumption.chart.title')}
            actions={
              <>
                <ChoiceChips
                  label={t('consumption.periodLabel')}
                  options={PERIODS.map((value) => ({ value, label: t(`consumption.periods.${value}`) }))}
                  value={period}
                  onChange={setPeriod}
                />
                <Button
                  size="sm"
                  variant="secondary"
                  icon={Download}
                  loading={exporting}
                  title={t('consumption.reportHint')}
                  onClick={exportReport}
                >
                  {exporting ? t('consumption.exporting') : t('consumption.report')}
                </Button>
              </>
            }
          >
            {renderChart()}
          </Card>
        }
        aside={
          <Card
            title={t('consumption.history.title')}
            actions={<span className="text-sm text-text-muted">{t('consumption.history.count', { count: tableList.total })}</span>}
          >
            <Filters>
              <Select aria-label={t('consumption.farmFilter')} value={farmFilter} onChange={(event) => setFarmFilter(event.target.value)} className="w-auto">
                <option value="">{t('common.allFarms')}</option>
                {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
              </Select>
              <Select aria-label={t('consumption.sourceFilter')} value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)} className="w-auto">
                <option value="">{t('consumption.allSources')}</option>
                {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
              </Select>
              <SearchField value={tableList.query} onChange={tableList.setQuery} placeholder={t('consumption.search')} />
            </Filters>
            {renderTable()}
          </Card>
        }
      />

      <QuotaPanel notify={notify} />
    </Screen>
  )
}
