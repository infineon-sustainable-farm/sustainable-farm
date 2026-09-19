import { useMemo, useState } from 'react'
import { useRainwaterHarvests, useWaterSources } from '../../hooks/useWaterData'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { WsRadialGauge, WsBarChart } from '../charts'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

const DAY_MS = 24 * 3600 * 1000

/**
 * Rainwater harvesting - READ-ONLY view.
 * Rainfall is measured by the rain gauge (IoT sensor) and harvest volumes
 * are computed automatically by the backend: no manual entry.
 */
export function RainwaterView() {
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
    let list = harvests.filter((item) => !sourceFilter || item.sourceId === sourceFilter)
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

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Rainwater harvesting</h1>
          <p>Rainfall measured by IoT sensor - harvest volumes computed automatically.</p>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div>
            <div className="label">Period</div>
            <div className="value">{period === 'all' ? 'All history' : period === 'week' ? 'Last week' : 'Last month'}</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Total collected</div>
            <div className="value" style={{ color: 'var(--ws-primary-dark)' }}>{Math.round(totalCollected)} L</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Coverage rate</div>
            <div className="value">{Math.round(coveragePct)} %</div>
          </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Tank level</h2>
            <span>{rainSource ? rainSource.name : '-'}</span>
          </div>
          <div className="ws-panel-body">
            <WsRadialGauge value={levelPct} color={levelPct >= 90 ? 'var(--ws-orange)' : 'var(--ws-primary)'} />
            <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--ws-muted)', textAlign: 'center' }}>
              Level measured by IoT sensor - orange alert above 90 percent (SC-06).
            </p>
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Harvest per rain event</h2>
            <div className="ws-switcher">
              {[['week', 'Week'], ['month', 'Month'], ['all', 'All']].map(([item, label]) => (
                <button key={item} className={`ws-chip ${period === item ? 'active' : ''}`} onClick={() => setPeriod(item)}>{label}</button>
              ))}
            </div>
          </div>
          <div className="ws-filters">
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">All sources</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
            <SearchInput value={tableList.query} onChange={tableList.setQuery} placeholder="Search by date..." />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading harvests..." full />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load harvests.'} />
            ) : barData.length === 0 ? (
              <EmptyState title="No harvest" description="Rainfall measured by the IoT rain gauge will appear here - volumes computed automatically." />
            ) : (
              <>
                <WsBarChart
                  data={barData}
                  xKey="day"
                  bars={[{ key: 'collected', name: 'Collected volume (L)', color: 'var(--ws-primary)' }]}
                  exportName="rainwater-harvest"
                />
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th onClick={() => tableList.toggleSort('captureDate')} style={{ cursor: 'pointer' }}>
                        Date {tableList.sort && tableList.sort.key === 'captureDate' ? (tableList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Collected volume</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableList.items.map((item) => (
                      <tr key={item.id}>
                        <td>{(item.captureDate || '').slice(0, 10)}</td>
                        <td><strong>{Math.round(item.harvestedLiters || 0)} L</strong></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <Pagination page={tableList.page} pageCount={tableList.pageCount} onPage={tableList.setPage} total={tableList.total} unit="harvest" />
              </>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
