import { useMemo, useState } from 'react'
import { useFarms } from '../../hooks/useFarms'
import { useWaterConsumptions, useWaterSources } from '../../hooks/useWaterData'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { WsAreaChart } from '../charts'
import { C } from '../chartUtils'
import { QuotaPanel } from './QuotaPanel'
import { downloadCsv } from '../../api/watersupplyApi'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

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
 * Consumption tracking — READ-ONLY view.
 * Volumes are measured by IoT sensors and reported automatically:
 * no manual entry (no create/edit/delete in the UI).
 */
export function ConsumptionView({ notify }) {
  const { consumptions, loading, error } = useWaterConsumptions()
  const { farms } = useFarms()
  const { sources } = useWaterSources()
  const [period, setPeriod] = useState('all')
  const [farmFilter, setFarmFilter] = useState('')
  const [sourceFilter, setSourceFilter] = useState('')
  const [exporting, setExporting] = useState(false)

  // Server-side export of the consumption report (CSV, current period) — P10.
  const exportReport = () => {
    setExporting(true)
    downloadCsv('consumption', period === 'all' ? 'month' : period)
      .then(() => notify?.('CSV report downloaded.'))
      .catch((err) => notify?.(err.message || 'Export failed.'))
      .finally(() => setExporting(false))
  }

  const filteredConsumptions = useMemo(() => consumptions.filter((item) => {
    if (farmFilter && item.farmId !== farmFilter) return false
    if (sourceFilter && item.sourceId !== sourceFilter) return false
    return true
  }), [consumptions, farmFilter, sourceFilter])

  // Search + sort + pagination for the table (the chart stays on the filtered data).
  const tableList = useListControls(filteredConsumptions, {
    searchFields: ['consumptionDate', 'createdAt'],
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

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Consumption tracking</h1>
          <p>Measurements reported automatically by IoT sensors - compared with the theoretical baseline.</p>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div>
            <div className="label">Selected period</div>
            <div className="value">{period === 'all' ? 'All history' : period === 'week' ? 'Last week' : 'Last month'}</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Total consumed</div>
            <div className="value" style={{ color: 'var(--ws-primary-dark)' }}>{totalLiters} L</div>
          </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Peak</div>
            <div className="value">{peakDay ? `${peakDay.day.slice(5)} - ${peakDay.liters} L` : '-'}</div>
          </div>
        </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Consumption over time</h2>
            <div className="ws-switcher">
              {[['week', 'Week'], ['month', 'Month'], ['all', 'All']].map(([item, label]) => (
                <button key={item} className={`ws-chip ${period === item ? 'active' : ''}`} onClick={() => setPeriod(item)}>
                  {label}
                </button>
              ))}
              <button className="ws-chip" onClick={exportReport} disabled={exporting} title="Download the consumption report (CSV)">
                {exporting ? 'Exporting…' : 'CSV report'}
              </button>
            </div>
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading consumptions…" />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load data.'} />
            ) : series.length === 0 ? (
              <EmptyState title="No consumption" description="IoT sensor measurements will appear here as soon as they are reported." />
            ) : (
              <WsAreaChart
                data={series}
                xKey="day"
                series={[{ key: 'liters', name: 'Volume (L)', color: C.primary }]}
                exportName="water-consumption"
              />
            )}
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header"><h2>Measurement history</h2><span>{tableList.total} measurement(s)</span></div>
          <div className="ws-filters">
            <select value={farmFilter} onChange={(event) => setFarmFilter(event.target.value)}>
              <option value="">All farms</option>
              {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
            </select>
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">All sources</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
            <SearchInput value={tableList.query} onChange={tableList.setQuery} placeholder="Search by date…" />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading measurements…" />
            ) : filteredConsumptions.length === 0 ? (
              <EmptyState title="No recorded consumption" description="Volumes are measured by IoT sensors and reported automatically - no manual entry." />
            ) : (
              <table className="ws-table">
                <thead>
                  <tr>
                    <th onClick={() => tableList.toggleSort('consumptionDate')} style={{ cursor: 'pointer' }}>
                      Date {tableList.sort && tableList.sort.key === 'consumptionDate' ? (tableList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                    </th>
                    <th>Volume</th>
                  </tr>
                </thead>
                <tbody>
                  {tableList.items.map((item) => (
                    <tr key={item.id}>
                      <td>{(item.consumptionDate || item.createdAt || '').slice(0, 10)}</td>
                      <td><strong>{item.consumptionLiters ?? 0} L</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <Pagination page={tableList.page} pageCount={tableList.pageCount} onPage={tableList.setPage} total={tableList.total} unit="measurement" />
          </div>
        </div>
      </div>

      <QuotaPanel notify={notify} />
    </>
  )
}
