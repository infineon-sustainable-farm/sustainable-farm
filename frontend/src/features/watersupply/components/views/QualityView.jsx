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
 * Qualite de l eau - vue en LECTURE SEULE.
 * Les mesures proviennent des sondes IoT (pH, turbidite, temperature, conductivite)
 * remontees via l endpoint d ingestion : aucune saisie manuelle.
 * Seuils : pH 6 - 7,5 et turbidite < 5 NTU -> alerte + notification automatique.
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
          <h1>Qualité de l'eau</h1>
          <p>Mesures des sondes IoT - alertes automatiques hors seuil : pH 6-7,5 - turbidite &lt; 5 NTU.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Historique des mesures</h2>
            <span>{alertsCount} alerte(s)</span>
          </div>
          <div className="ws-filters">
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">Toutes les sources</option>
              {sources.map((source) => <option key={source.id} value={source.id}>{source.name}</option>)}
            </select>
            <SearchInput value={list.query} onChange={list.setQuery} placeholder="Rechercher par date..." />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Chargement des mesures..." full />
            ) : error ? (
              <EmptyState title="Erreur" description={error.message || 'Impossible de charger les mesures.'} />
            ) : sourceFiltered.length === 0 ? (
              <EmptyState title="Aucune mesure" description="Les mesures des sondes IoT (pH, turbidite) apparaitront ici des leur remontee." />
            ) : (
              <table className="ws-table">
                <thead>
                  <tr>
                    <th onClick={() => list.toggleSort('testDate')} style={{ cursor: 'pointer' }}>
                      Date {list.sort && list.sort.key === 'testDate' ? (list.sort.dir === 'asc' ? '↑' : '↓') : ''}
                    </th>
                    <th>Source</th>
                    <th>pH</th>
                    <th>Turbidité</th>
                    <th>Statut</th>
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
                        <td><span className={`ws-tag ${out ? 'red' : 'green'}`}>{out ? 'Alerte' : 'Conforme'}</span></td>
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
            <div className="ws-panel-header"><h2>Seuils de reference</h2></div>
            <div className="ws-panel-body">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>pH conforme</span>
                  <strong>6,0 - 7,5</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>Turbidité maximale</span>
                  <strong>5 NTU</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--ws-muted)' }}>Hors seuil</span>
                  <span className="ws-tag red">Alerte + notification automatique</span>
                </div>
              </div>
            </div>
          </div>

          <div className="ws-panel">
            <div className="ws-panel-header"><h2>Source des données</h2></div>
            <div className="ws-panel-body">
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--ws-muted)', lineHeight: 1.6 }}>
                Les mesures proviennent des sondes IoT installees sur les sources.
                Elles remontent automatiquement via l endpoint d ingestion - aucune saisie manuelle.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
