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
          <h1>Drought alerts</h1>
          <p>Risk level based on reservoir levels, season and operational demand.</p>
        </div>
      </div>

      {loading ? (
        <Spinner label="Loading…" full />
      ) : error ? (
        <EmptyState title="Error" description={error.message || 'Unable to load.'} />
      ) : data ? (
        <div className="ws-layout-2">
          <div className="ws-panel">
            <div className="ws-panel-header">
              <h2>Current risk gauge</h2>
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
              <div className="ws-panel-header">              <h2>Recommendation</h2></div>
              <div className="ws-panel-body">
                <span className={`ws-tag ${meta.tag}`}>{meta.text}</span>
                <div style={{ display: 'flex', gap: '22px', marginTop: 12, marginBottom: 4, fontSize: '13px' }}>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Reserve days</div>
                    <strong>{data.days_of_reserve_remaining != null ? (Math.round(data.days_of_reserve_remaining * 10) / 10) + ' j' : 'n/d'}</strong>
                  </div>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Avg. consumption (7 d)</div>
                    <strong>{data.daily_average_consumption_liters_7d ?? 0} L/j</strong>
                  </div>
                  <div>
                    <div style={{ color: 'var(--ws-muted)', fontSize: '11px' }}>Reserve</div>
                    <strong>{data.total_reserve_liters ?? 0} / {data.total_capacity_liters ?? 0} L</strong>
                  </div>
                </div>
                <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55, marginTop: 8 }}>
                  {data.recommendations || 'Reserve water for priority crops.'}
                </p>
                <p style={{ color: 'var(--ws-muted)', fontSize: '12px' }}>
                  Prediction from {data.prediction_date ? new Date(data.prediction_date).toLocaleString() : '—'} · confidence {(data.confidence ?? 0) * 100}%
                </p>
              </div>
            </div>
            <div className="ws-panel">
              <div className="ws-panel-header">              <h2>Critical automation</h2></div>
              <div className="ws-panel-body">
                <span className="ws-tag red">If critical</span>
                <p style={{ color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                  Proposes postponing non-essential irrigation until the reservoir recovers.
                </p>
                <button className="ws-chip active" onClick={() => notify('Postponement plan prepared (demo).')}>Prepare postponement plan</button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <EmptyState title="No prediction" />
      )}
    </>
  )
}