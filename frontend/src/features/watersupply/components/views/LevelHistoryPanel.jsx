import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, ErrorState } from '../../../../shared/ui'
import { useLevelHistory } from '../../hooks/useIntegrations'
import { useWaterFormat } from '../../format'
import { WsAreaChart } from '../charts'
import { C } from '../chartUtils'
import { LoadingState } from '../common/LoadingState'

/**
 * Level history of a source: every sensor reading and every manual level change. The source itself
 * only keeps its last level, so this is the only place where a tank can be followed over time.
 */
export function LevelHistoryPanel({ source, onClose }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { history, loading, error } = useLevelHistory(source.id)
  const series = useMemo(
    () => history.map((reading) => ({ time: format.instant(reading.measuredAt), liters: Math.round(reading.levelLiters) })),
    [history, format],
  )

  function renderContent() {
    if (loading) return <LoadingState compact label={t('sources.history.loading')} />
    if (error) return <ErrorState message={error.message || t('sources.history.loadError')} />
    if (series.length === 0) {
      return <EmptyState title={t('sources.history.emptyTitle')} message={t('sources.history.empty')} />
    }
    return (
      <WsAreaChart
        data={series}
        xKey="time"
        series={[{ key: 'liters', name: t('sources.history.series'), color: C.primary }]}
        referenceLines={source.capacityLiters ? [{ value: source.capacityLiters, label: t('sources.history.capacity'), color: C.orange }] : []}
        exportName={`level-history-${source.name}`}
      />
    )
  }

  return (
    <Card
      title={t('sources.history.title', { name: source.name })}
      actions={<Button size="sm" variant="secondary" onClick={onClose}>{t('common.close')}</Button>}
    >
      {renderContent()}
    </Card>
  )
}
