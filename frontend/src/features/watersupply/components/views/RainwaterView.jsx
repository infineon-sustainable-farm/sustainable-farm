import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, EmptyState, ErrorState, Select, Table } from '../../../../shared/ui'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { useRainwaterHarvests, useWaterSources } from '../../hooks/useWaterData'
import { useWaterFormat } from '../../format'
import { WsRadialGauge, WsBarChart } from '../charts'
import { C } from '../chartUtils'
import { ChoiceChips } from '../common/ChoiceChips'
import { Filters, Screen, SplitLayout } from '../common/Screen'
import { ListPagination } from '../common/ListPagination'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { Stat } from '../common/Stat'
import { tableSort } from '../common/listSort'
import { sameId } from './ids'

const DAY_MS = 24 * 3600 * 1000
const PERIODS = ['week', 'month', 'all']

/**
 * Rainwater harvesting - READ-ONLY view.
 * Rainfall is measured by the rain gauge (IoT sensor) and harvest volumes
 * are computed automatically by the backend: no manual entry.
 */
export function RainwaterView() {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { harvests, loading, error } = useRainwaterHarvests()
  const { sources } = useWaterSources()
  const [period, setPeriod] = useState('all')
  const [sourceFilter, setSourceFilter] = useState('')

  const maxDateMs = useMemo(() => {
    let max = 0
    for (const item of harvests) {
      const time = new Date(item.captureDate || item.createdAt).getTime()
      if (!Number.isNaN(time) && time > max) max = time
    }
    return max
  }, [harvests])

  const filtered = useMemo(() => {
    let list = harvests.filter((item) => !sourceFilter || sameId(item.sourceId, sourceFilter))
    if (period !== 'all') {
      const days = period === 'week' ? 7 : 30
      const cutoff = maxDateMs - days * DAY_MS
      list = list.filter((item) => new Date(item.captureDate || item.createdAt).getTime() >= cutoff)
    }
    return list
  }, [harvests, sourceFilter, period, maxDateMs])

  const tableList = useListControls(filtered, {
    searchFields: ['captureDate'],
    defaultSort: { key: 'captureDate', dir: 'desc' },
  })

  const rainSource = useMemo(() => sources.find((source) => (source.type || '').toLowerCase().includes('rain')) || sources[0], [sources])
  const levelPct = rainSource?.capacityLiters ? (rainSource.currentLevelLiters / rainSource.capacityLiters) * 100 : 0
  const totalCollected = useMemo(() => filtered.reduce((sum, item) => sum + (item.harvestedLiters || 0), 0), [filtered])
  const coveragePct = rainSource?.capacityLiters ? Math.min(100, (totalCollected / rainSource.capacityLiters) * 100) : 0
  const barData = useMemo(() => filtered.slice(0, 8).map((item) => ({
    day: (item.captureDate || '').slice(0, 10),
    collected: Math.round(item.harvestedLiters || 0),
  })), [filtered])

  const columns = [
    { key: 'captureDate', header: t('rainwater.columns.date'), sortable: true, cell: (item) => (item.captureDate || '').slice(0, 10) },
    { key: 'volume', header: t('rainwater.columns.volume'), cell: (item) => <strong>{format.liters(Math.round(item.harvestedLiters || 0))}</strong> },
  ]

  function renderHarvests() {
    if (loading) return <LoadingState label={t('rainwater.loading')} />
    if (error) return <ErrorState message={error.message || t('rainwater.loadError')} />
    if (barData.length === 0) return <EmptyState title={t('rainwater.emptyTitle')} message={t('rainwater.empty')} />
    return (
      <div className="flex flex-col gap-4">
        <WsBarChart
          data={barData}
          xKey="day"
          bars={[{ key: 'collected', name: t('rainwater.series'), color: C.primary }]}
          exportName="rainwater-harvest"
        />
        <Table caption={t('rainwater.harvestTitle')} columns={columns} rows={tableList.items} {...tableSort(tableList)} />
        <ListPagination list={tableList} />
      </div>
    )
  }

  return (
    <Screen title={t('rainwater.title')} subtitle={t('rainwater.subtitle')}>
      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label={t('rainwater.period')} value={t(`rainwater.periodValues.${period}`)} />
        <Stat label={t('rainwater.totalCollected')} value={<span className="text-text-accent">{format.liters(Math.round(totalCollected))}</span>} />
        <Stat label={t('rainwater.coverage')} value={t('rainwater.coverageValue', { value: format.number(Math.round(coveragePct)) })} />
      </div>

      <SplitLayout
        main={
          <Card
            title={t('rainwater.tankTitle')}
            actions={<span className="text-sm text-text-muted">{rainSource ? rainSource.name : t('rainwater.noSource')}</span>}
          >
            <WsRadialGauge value={levelPct} color={levelPct >= 90 ? C.orange : C.primary} />
            <p className="mt-3 text-center text-xs text-text-muted">{t('rainwater.tankNote')}</p>
          </Card>
        }
        aside={
          <Card
            title={t('rainwater.harvestTitle')}
            actions={
              <ChoiceChips
                label={t('rainwater.periodLabel')}
                value={period}
                onChange={setPeriod}
                options={PERIODS.map((value) => ({ value, label: t(`rainwater.periods.${value}`) }))}
              />
            }
          >
            <Filters>
              <Select aria-label={t('rainwater.sourceFilter')} value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)} className="w-auto">
                <option value="">{t('rainwater.allSources')}</option>
                {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
              </Select>
              <SearchField value={tableList.query} onChange={tableList.setQuery} placeholder={t('rainwater.search')} />
            </Filters>
            {renderHarvests()}
          </Card>
        }
      />
    </Screen>
  )
}
