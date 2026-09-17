import { useDroughtPrediction } from '../../hooks/useDashboard'
import { WsRadialGauge } from '../charts'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

function riskMeta(risk) {
  switch ((risk || '').toUpperCase()) {
    case 'CRITICAL': return { step: 4, cls: 'red', tag: 'critical', text: 'Critical' }
    case 'HIGH': return { step: 3, cls: 'orange', tag: 'red', text: 'Alert' }
    case 'MEDIUM': return { step: 2, cls: 'orange', tag: 'orange', text: 'Vigilance' }
    default: return { step: 1, cls: '', tag: 'green', text: 'Normal' }
  }
}

const STEPS = ['Normal', 'Vigilance', 'Alert', 'Critical']

export function DroughtView({ notify }) {
  const { data, loading, error } = useDroughtPrediction()
  const meta = riskMeta(data?.risk_level)

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Alertes de sécheresse</h1>
          <p>Niveau de risque basé sur le niveau des réservoirs, la saison et la demande opérationnelle.</p>
        </div>
      </div>

      {loading ? (
        <Spinner label="Chargement…" full />
      ) : error ? (
        <EmptyState title="Erreur" description={error.message || 'Impossible de charger.'} />
      ) : data ? (
        <div className="ws-layout-2">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Jauge de risque actuelle</h2>
              <span className={`ws-tag ${meta.tag}`}>{meta.text}</span>
            </div>
            <div className="ws-panel-body">
              <WsRadialGauge
                value={data.reservoir_level_percentage ?? 0}
                color={
                  meta.step >= 3
                    ? 'var(--ws-red, #c62828)'
                    : meta.step === 2
                      ? 'var(--ws-orange, #ef6c00)'
                      : 'var(--ws-green, #4caf50)'
                }
              />
              <div className="ws-risk-gauge">
                {STEPS.map((label, i) => (
                  <div key={label} className={`ws-risk-step ${meta.step === i + 1 ? 'active' : ''}`}>
                    {label}
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="ws-stack">
            <div className="ws-panel">
              <div className="ws-panel-header"><h2>Recommandation</h2></div>
              <div className="ws-panel-body">
                <span className={`ws-tag ${meta.tag}`}>{meta.text}</span>
                <div style={{ display: 'flex', gap: '22px', marginTop: 12, marginBottom: 4, fontSize: '13px' }}>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Jours de réserve</div>
                    <strong>{data.days_of_reserve_remaining != null ? (Math.round(data.days_of_reserve_remaining * 10) / 10) + ' j' : 'n/d'}</strong>
                  </div>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Consommation moy. (7 j)</div>
                    <strong>{data.daily_average_consumption_liters_7d ?? 0} L/j</strong>
                  </div>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Réserve</div>
                    <strong>{data.total_reserve_liters ?? 0} / {data.total_capacity_liters ?? 0} L</strong>
                  </div>
                </div>
                <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55, marginTop: 8 }}>
                  {data.recommendations || 'Réservez l\'eau aux cultures prioritaires.'}
                </p>
                <p style={{ color: 'var(--ws-muted)', fontSize: '12px' }}>
                  Prédiction du {data.prediction_date ? new Date(data.prediction_date).toLocaleString() : '—'} · confiance {(data.confidence ?? 0) * 100}%
                </p>
              </div>
            </div>
            <div className="ws-panel">
              <div className="ws-panel-header"><h2>Automatisation critique</h2></div>
              <div className="ws-panel-body">
                <span className="ws-tag red">If critical</span>
                <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                  Proposez le report des irrigations non-essentielles jusqu'au rétablissement du réservoir.
                </p>
                <button className="ws-chip active" onClick={() => notify('Plan de report préparé (démo).')}>Preparer le plan de report</button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <EmptyState title="Aucune prédiction" />
      )}
    </>
  )
}