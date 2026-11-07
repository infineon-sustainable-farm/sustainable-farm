import { useMemo } from 'react'
import { useLevelHistory } from '../../hooks/useIntegrations'
import { WsAreaChart } from '../charts'
import { C } from '../chartUtils'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from './EmptyState'
import { formatInstant } from './datetime'

/**
 * Level history of a source: every sensor reading and every manual level change. The source itself
 * only keeps its last level, so this is the only place where a tank can be followed over time.
 */
export function LevelHistoryPanel({ source, onClose }) {
  const { history, loading, error } = useLevelHistory(source.id)
  const series = useMemo(
    () => history.map((reading) => ({ time: formatInstant(reading.measuredAt), liters: Math.round(reading.levelLiters) })),
    [history],
  )

  return (
    <div className="ws-panel" style={{ marginTop: '22px' }}>
      <div className="ws-panel-header">
        <h2>Level history - {source.name}</h2>
        <button className="ws-chip" onClick={onClose}>Close</button>
      </div>
      <div className="ws-panel-body">
        {loading ? (
          <Spinner label="Loading the level history…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load the history.'} />
        ) : series.length === 0 ? (
          <EmptyState title="No level recorded" description="Readings appear here as the level sensor reports them or when the level is edited." />
        ) : (
          <WsAreaChart
            data={series}
            xKey="time"
            series={[{ key: 'liters', name: 'Level (L)', color: C.primary }]}
            referenceLines={source.capacityLiters ? [{ value: source.capacityLiters, label: 'Capacity', color: C.orange }] : []}
            exportName={`level-history-${source.name}`}
          />
        )}
      </div>
    </div>
  )
}
