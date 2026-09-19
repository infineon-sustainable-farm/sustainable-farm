import { useMemo, useState } from 'react'
import { useWaterQualityTests, useWaterSources } from '../../hooks/useWaterData'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

function isOutOfRange(test) {
  return (test.ph != null && (test.ph < 6 || test.ph > 7.5)) || (test.turbidityNtu != null && test.turbidityNtu > 5)
}

/**
 * Water quality - READ-ONLY view.
 * Measurements come from IoT probes (pH, turbidity, temperature, conductivity)
 * reported through the ingestion endpoint: no manual entry.
 * Thresholds: pH 6 - 7.5 and turbidity < 5 NTU -> alert + automatic notification.
 */
export function QualityView() {
  const { tests, loading, error } = useWaterQualityTests()
  const { sources } = useWaterSources()
  const [sourceFilter, setSourceFilter] = useState('')

  const sourceFiltered = useMemo(
    () => tests
      .filter((test) => !sourceFilter || test.sourceId === sourceFilter)
      .sort((a, b) => Number(isOutOfRange(b)) - Number(isOutOfRange(a))),
    [tests, sourceFilter],
  )

  const list = useListControls(sourceFiltered, { searchFields: ['testDate'] })
  const filtered = list.items
  const alertsCount = useMemo(() => sourceFiltered.filter(isOutOfRange).length, [sourceFiltered])

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water quality</h1>
          <p>IoT probe measurements - automatic out-of-range alerts: pH 6-7.5 - turbidity &lt; 5 NTU.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Measurement history</h2>
            <span>{alertsCount} alert(s)</span>
          </div>
          <div className="ws-filters">
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">All sources</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
            <SearchInput value={list.query} onChange={list.setQuery} placeholder="Search by date..." />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading measurements..." full />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load measurements.'} />
            ) : sourceFiltered.length === 0 ? (
              <EmptyState title="No measurements" description="IoT probe measurements (pH, turbidity) will appear here as soon as they are reported." />
            ) : (
              <table className="ws-table">
                <thead>
                  <tr>
                    <th onClick={() => list.toggleSort('testDate')} style={{ cursor: 'pointer' }}>
                      Date {list.sort && list.sort.key === 'testDate' ? (list.sort.dir === 'asc' ? '↑' : '↓') : ''}
                    </th>
                    <th>Source</th>
                    <th>pH</th>
                    <th>Turbidity</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((test) => {
                    const out = isOutOfRange(test)
                    return (
                      <tr key={test.id}>
                        <td>{(test.testDate || '').replace('T', ' ').slice(0, 16)}</td>
                        <td>{sources.find((source) => source.id === test.sourceId)?.name || test.sourceId?.slice(0, 8) || '-'}</td>
                        <td><strong style={{ color: test.ph < 6 || test.ph > 7.5 ? 'var(--ws-red)' : undefined }}>{test.ph ?? '-'}</strong></td>
                        <td><strong style={{ color: test.turbidityNtu > 5 ? 'var(--ws-orange)' : undefined }}>{test.turbidityNtu ?? '-'} NTU</strong></td>
                        <td><span className={`ws-tag ${out ? 'red' : 'green'}`}>{out ? 'Alert' : 'Compliant'}</span></td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
            <Pagination page={list.page} pageCount={list.pageCount} onPage={list.setPage} total={list.total} unit="mesure" />
          </div>
        </div>

        <div className="ws-stack">
          <div className="ws-panel">
            <div className="ws-panel-header">            <h2>Reference thresholds</h2></div>
            <div className="ws-panel-body">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>Compliant pH</span>
                  <strong>6.0 - 7.5</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>Maximum turbidity</span>
                  <strong>5 NTU</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>Out of range</span>
                  <span className="ws-tag red">Alert + automatic notification</span>
                </div>
              </div>
            </div>
          </div>

          <div className="ws-panel">
            <div className="ws-panel-header">            <h2>Data source</h2></div>
            <div className="ws-panel-body">
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--ws-muted)', lineHeight: 1.6 }}>
                Measurements come from IoT probes installed on the sources.
                They are reported automatically via the ingestion endpoint - no manual entry.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
