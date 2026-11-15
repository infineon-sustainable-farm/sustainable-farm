import { useKpis, useAlerts, useActivities, useRecommendations, useDroughtPrediction, useWeather, useHealthCheck, useWaterBalance } from '../../hooks/useDashboard'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from './EmptyState'

/**
 * Dashboard connected to the API (Task 2).
 * Replaces frozen data with real data through the useDashboard hooks,
 * keeping the .ws-* design system (colors/typography unchanged).
 * Handles loading / error / empty states.
 */
export function DashboardView({ onNavigate, notify }) {
  const { data: kpis, loading: kpisLoading, error: kpisError } = useKpis()
  const { data: alerts, loading: alertsLoading } = useAlerts()
  const { data: activities, loading: activitiesLoading } = useActivities()
  const { data: recommendations, loading: recLoading } = useRecommendations()
  const { data: drought, loading: droughtLoading } = useDroughtPrediction()
  const { data: weather, loading: weatherLoading } = useWeather()
  const { healthy: backendHealthy, loading: healthLoading } = useHealthCheck()

  const anythingLoading = kpisLoading || alertsLoading || activitiesLoading || weatherLoading

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water dashboard</h1>
          <p>Operational view: irrigation, tanks, quality and drought risk.</p>
        </div>
      </div>

      {/* Backend status */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '18px', fontFamily: "'Inter', Arial, sans-serif", fontSize: '13px', color: 'var(--ws-muted)' }}>
        <span
          className={`ws-summary-dot ${backendHealthy ? 'green' : 'red'}`}
          style={{ width: '10px', height: '10px', borderRadius: '50%', display: 'inline-block' }}
        />
        <span>
          {healthLoading
            ? 'Checking backend…'
            : backendHealthy
              ? 'Backend connected'
              : 'Backend disconnected'}
        </span>
      </div>

      {anythingLoading ? (
        <Spinner label="Loading dashboard…" full />
      ) : kpisError ? (
        <EmptyState
          title="Unable to load data"
          description={kpisError.message || 'Make sure the backend is running, then try again.'}
        />
      ) : kpis ? (
        <>
          {/* ===== KPI ===== */}
          <div className="ws-kpi-grid">
            <Kpi label="Daily consumption" value={`${kpis.daily_consumption_liters ?? 0} L`} />
            <Kpi label="Monthly consumption" value={`${kpis.monthly_consumption_liters ?? 0} L`} />
            <Kpi label="Tank level" value={`${kpis.tank_level_percentage ?? 0}%`} tone="orange" />
            <Kpi label="Water savings" value={`${kpis.water_savings_percentage ?? 0}%`} tone="hero" />
            <Kpi label="Irrigations today" value={kpis.irrigation_count_today ?? 0} />
            <Kpi label="Anomalies" value={kpis.anomaly_count ?? 0} tone="orange" />
            <Kpi label="Water quality" value={kpis.water_quality_status ?? '—'} />
            <Kpi label="Sensors available" value={`${kpis.sensor_availability_percentage ?? 0}%`} />
          </div>
{/* ===== Weather ===== */}
          {weather && (
            <div className="ws-panel" style={{ marginTop: '22px' }}>
              <div className="ws-panel-header">
                <h2>Current weather</h2>
                <span>Open-Meteo</span>
              </div>
              <div className="ws-panel-body">
                <div className="ws-row" style={{ gridTemplateColumns: 'repeat(4, minmax(0,1fr))' }}>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.temperature ?? '—'}°C</strong><small>Temperature</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.humidity ?? '—'}%</strong><small>Humidity</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.wind_speed ?? '—'} km/h</strong><small>Wind</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.weather_condition ?? '—'}</strong><small>Condition</small></div>
                </div>
              </div>
            </div>
          )}

          {/* ===== Main content ===== */}
          <div className="ws-dashboard-grid" style={{ marginTop: '22px' }}>
            <div className="ws-stack">
              {recLoading ? (
                <Spinner label="Recommendations…" />
              ) : recommendations && recommendations.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Recommendations</h2>
                    <span>Priority-ordered</span>
                  </div>
                  <div className="ws-panel-body ws-summary-list">
                    {recommendations.map((rec, i) => (
                      <div className="ws-summary-item" key={i}>
                        <span className={`ws-summary-dot ${recPriorityDot(rec.priority)}`} />
                        <div className="ws-summary-title">
                          <span className={`ws-tag ${recTag(rec.priority)}`}>{rec.priority}</span>
                          <div>
                            <small style={{ marginTop: '4px' }}>{rec.recommendation}</small>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <EmptyState title="No recommendations" description="Nothing to display right now." />
              )}

              {activitiesLoading ? (
                <Spinner label="Activities…" />
              ) : activities && activities.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Recent activity</h2>
                    <span>History</span>
                  </div>
                  <div className="ws-panel-body ws-summary-list">
                    {activities.map((a, i) => (
                      <div className="ws-summary-item" key={i}>
                        <span className="ws-summary-dot green" />
                        <div className="ws-summary-title">
                          {a.message}
                          <small style={{ marginTop: '4px' }}>{a.timestamp}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <EmptyState title="No activity" description="Recorded consumptions will appear here." />
              )}
            </div>
<div className="ws-stack">
              {!droughtLoading && drought && (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Drought forecast</h2>
                    <span>Outlook</span>
                  </div>
                  <div className="ws-panel-body">
                    <div className={`ws-tag ${droughtRiskTag(drought.risk_level)}`} style={{ marginBottom: '8px' }}>
                      {drought.risk_level}
                    </div>
                    <p style={{ margin: 0, fontFamily: "'Inter', Arial, sans-serif", fontSize: '13px', color: 'var(--ws-muted)', lineHeight: 1.55 }}>
                      {drought.recommendations}
                    </p>
                  </div>
                </div>
              )}

              <WaterBalancePanel />

              {alertsLoading ? (
                <Spinner label="Alerts…" />
              ) : alerts && alerts.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Recent alerts</h2>
                    <span>Unread</span>
                  </div>
                  <div className="ws-panel-body ws-summary-list">
                    {alerts.map((al, i) => (
                      <div className="ws-summary-item" key={i}>
                        <span className={`ws-summary-dot ${alertDot(al.type)}`} />
                        <div className="ws-summary-title">
                          {al.title}
                          <small style={{ marginTop: '4px' }}>{al.message}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <EmptyState title="No alerts" description="Everything is nominal." />
              )}

              <div className="ws-panel">
                <div className="ws-panel-header">
                  <h2>Actions</h2>
                </div>
                <div className="ws-panel-body">
                  <div className="ws-quick-actions">
                    <button className="ws-action-btn" onClick={() => onNavigate('irrigation')}>Manage irrigation</button>
                    <button className="ws-action-btn secondary" onClick={() => onNavigate('consumption')}>View consumption</button>
                    <button className="ws-action-btn secondary" onClick={() => onNavigate('quality')}>View water quality</button>
                    <button className="ws-action-btn secondary" onClick={() => onNavigate('outages')}>Plan an outage</button>
                    <button className="ws-action-btn warning" onClick={() => notify('See the Drought view for the plan.')}>Drought plan</button>
                    <button className="ws-action-btn secondary" onClick={() => notify('Demo: report prepared.')}>Prepared carry-over plan</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : (
        <EmptyState title="No data" description="Add data to visualize the dashboard." />
      )}
    </>
  )
}

/**
 * Water balance of the month and what the tanks will hold once the irrigations scheduled for the
 * next 7 days have run (demand_forecast_liters / available_after_forecast_liters).
 */
function WaterBalancePanel() {
  const { data: balance, loading, error } = useWaterBalance('month')
  if (loading || error || !balance) return null
  const available = balance.available_after_forecast_liters ?? 0
  return (
    <div className="ws-panel">
      <div className="ws-panel-header">
        <h2>Water balance</h2>
        <span className={`ws-tag ${balance.balance_status === 'deficit' ? 'red' : balance.balance_status === 'unknown' ? 'primary' : 'green'}`}>
          {balance.balance_status === 'deficit' ? 'Deficit' : balance.balance_status === 'unknown' ? 'No tank capacity' : 'Balanced'}
        </span>
      </div>
      <div className="ws-panel-body ws-summary-list">
        <div className="ws-summary-item">
          <span className="ws-summary-dot green" />
          <div className="ws-summary-title">Rain harvested this month<small>{balance.rainwater_in_liters ?? 0} L</small></div>
        </div>
        <div className="ws-summary-item">
          <span className="ws-summary-dot orange" />
          <div className="ws-summary-title">Irrigation this month<small>{balance.irrigation_out_liters ?? 0} L</small></div>
        </div>
        <div className="ws-summary-item">
          <span className="ws-summary-dot green" />
          <div className="ws-summary-title">
            In the tanks now
            <small>{balance.reserve_level_liters ?? 0} L of {balance.reserve_capacity_liters ?? 0} L ({balance.reserve_level_percentage ?? 0}%)</small>
          </div>
        </div>
        <div className="ws-summary-item">
          <span className={`ws-summary-dot ${available < 0 ? 'red' : 'orange'}`} />
          <div className="ws-summary-title">
            After the next 7 days of scheduled irrigation
            <small>{balance.demand_forecast_liters ?? 0} L planned, {available} L left</small>
          </div>
        </div>
      </div>
    </div>
  )
}

function Kpi({ label, value, tone }) {
  return (
    <div className={`ws-kpi-card ${tone || ''}`}>
      <div className="ws-kpi-label">{label}</div>
      <div className="ws-kpi-value">{value}</div>
    </div>
  )
}

function recPriorityDot(p) {
  switch (p) {
    case 'critical': return 'red'
    case 'high': case 'medium': return 'orange'
    case 'low': return 'green'
    default: return 'green'
  }
}

function recTag(p) {
  switch (p) {
    case 'critical': case 'high': return 'red'
    case 'medium': return 'orange'
    case 'low': return 'green'
    default: return 'primary'
  }
}

function droughtRiskTag(r) {
  switch ((r || '').toUpperCase()) {
    case 'CRITICAL': case 'HIGH': return 'red'
    case 'MEDIUM': return 'orange'
    case 'LOW': return 'green'
    default: return 'primary'
  }
}

function alertDot(type) {
  switch (type) {
    case 'warning': case 'critical': return 'red'
    default: return 'green'
  }
}
