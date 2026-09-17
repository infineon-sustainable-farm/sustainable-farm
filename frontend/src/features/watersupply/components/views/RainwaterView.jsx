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
 * Recuperation d eau de pluie - vue en LECTURE SEULE.
 * Les pluies sont mesurees par le pluviometre (capteur IoT) et les volumes
 * de recolte sont calcules automatiquement par le backend : aucune saisie manuelle.
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
          <h1>Récupération d'eau de pluie</h1>
          <p>Pluviometrie mesuree par capteur IoT - volumes de recolte calcules automatiquement.</p>
        </div>
      </div>

      <div className="ws-metric-banner">
        <div className="ws-metric-pill">
          <div>
            <div className="label">Période</div>
            <div className="value">{period === 'all' ? 'Tout historique' : period === 'week' ? 'Dernière semaine' : 'Dernier mois'}</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Total collecte</div>
            <div className="value" style={{ color: 'var(--ws-primary-dark)' }}>{Math.round(totalCollected)} L</div>
          </div>
        </div>
        <div className="ws-metric-pill">
          <div>
            <div className="label">Taux de couverture</div>
            <div className="value">{Math.round(coveragePct)} %</div>
          </div>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Niveau de la cuve</h2>
            <span>{rainSource ? rainSource.name : '-'}</span>
          </div>
          <div className="ws-panel-body">
            <WsRadialGauge value={levelPct} color={levelPct >= 90 ? 'var(--ws-orange)' : 'var(--ws-primary)'} />
            <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--ws-muted)', textAlign: 'center' }}>
              Niveau mesure par capteur IoT - alerte orange au-dela de 90 pourcent (SC-06).
            </p>
          </div>
        </div>

        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Recolte par evenement de pluie</h2>
            <div className="ws-switcher">
              {[['week', 'Semaine'], ['month', 'Mois'], ['all', 'Tout']].map(([item, label]) => (
                <button key={item} className={`ws-chip ${period === item ? 'active' : ''}`} onClick={() => setPeriod(item)}>{label}</button>
              ))}
            </div>
          </div>
          <div className="ws-filters">
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">Toutes les sources</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
            <SearchInput value={tableList.query} onChange={tableList.setQuery} placeholder="Rechercher par date..." />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Chargement des recoltes..." full />
            ) : error ? (
              <EmptyState title="Erreur" description={error.message || 'Impossible de charger les recoltes.'} />
            ) : barData.length === 0 ? (
              <EmptyState title="Aucune recolte" description="Les pluies mesurees par le pluviometre IoT apparaitront ici - volumes calcules automatiquement." />
            ) : (
              <>
                <WsBarChart
                  data={barData}
                  xKey="day"
                  bars={[{ key: 'collected', name: 'Volume collecte (L)', color: 'var(--ws-primary)' }]}
                  exportName="recolte-pluviale"
                />
                <table className="ws-table">
                  <thead>
                    <tr>
                      <th onClick={() => tableList.toggleSort('captureDate')} style={{ cursor: 'pointer' }}>
                        Date {tableList.sort && tableList.sort.key === 'captureDate' ? (tableList.sort.dir === 'asc' ? '↑' : '↓') : ''}
                      </th>
                      <th>Volume collecte</th>
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
                <Pagination page={tableList.page} pageCount={tableList.pageCount} onPage={tableList.setPage} total={tableList.total} unit="recolte" />
              </>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
