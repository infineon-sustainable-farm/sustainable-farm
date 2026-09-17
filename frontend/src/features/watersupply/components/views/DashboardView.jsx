import { useKpis, useAlerts, useActivities, useRecommendations, useDroughtPrediction, useWeather, useHealthCheck } from '../../hooks/useDashboard'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

/**
 * Dashboard connecté à l'API (Tâche 2).
 * Remplace les données figées par des données réelles via les hooks useDashboard,
 * en conservant le design system .ws-* (couleurs/typo inchangées).
 * Gère les états loading / error / empty.
 */
export function DashboardView({ onNavigate, notify }) {
  const { data: kpis, loading: kpisLoading, error: kpisError } = useKpis()
  const { data: alerts, loading: alertsLoading } = useAlerts()
  const { data: activities, loading: activitiesLoading } = useActivities()
  const { data: recommendations, loading: recLoading } = useRecommendations()
  const { data: drought, loading: droughtLoading } = useDroughtPrediction()
  const { data: weather, loading: weatherLoading } = useWeather(10.5, -61.2)
  const { healthy: backendHealthy, loading: healthLoading } = useHealthCheck()

  const anythingLoading = kpisLoading || alertsLoading || activitiesLoading || weatherLoading

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Tableau de bord de l'eau</h1>
          <p>Vue opérationnelle : irrigation, réservoirs, qualité et risque de sécheresse.</p>
        </div>
      </div>

      {/* Statut backend */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '18px', fontFamily: "'Inter', Arial, sans-serif", fontSize: '13px', color: 'var(--ws-muted)' }}>
        <span
          className={`ws-summary-dot ${backendHealthy ? 'green' : 'red'}`}
          style={{ width: '10px', height: '10px', borderRadius: '50%', display: 'inline-block' }}
        />
        <span>
          {healthLoading
            ? 'Vérification du backend…'
            : backendHealthy
              ? 'Backend connecté'
              : 'Backend déconnecté'}
        </span>
      </div>

      {anythingLoading ? (
        <Spinner label="Chargement du dashboard…" full />
      ) : kpisError ? (
        <EmptyState
          title="Impossible de charger les données"
          description={kpisError.message || 'Vérifiez que le backend est démarré puis réessayez.'}
        />
      ) : kpis ? (
        <>
          {/* ===== KPI ===== */}
          <div className="ws-kpi-grid">
            <Kpi label="Consommation quotidienne" value={`${kpis.daily_consumption_liters ?? 0} L`} />
            <Kpi label="Consommation mensuelle" value={`${kpis.monthly_consumption_liters ?? 0} L`} />
            <Kpi label="Niveau réservoirs" value={`${kpis.tank_level_percentage ?? 0}%`} tone="orange" />
            <Kpi label="Économie d'eau" value={`${kpis.water_savings_percentage ?? 0}%`} tone="hero" />
            <Kpi label="Irrigations aujourd'hui" value={kpis.irrigation_count_today ?? 0} />
            <Kpi label="Anomalies" value={kpis.anomaly_count ?? 0} tone="orange" />
            <Kpi label="Qualité de l'eau" value={kpis.water_quality_status ?? '—'} />
            <Kpi label="Capteurs dispo." value={`${kpis.sensor_availability_percentage ?? 0}%`} />
          </div>
{/* ===== Météo ===== */}
          {weather && (
            <div className="ws-panel" style={{ marginTop: '22px' }}>
              <div className="ws-panel-header">
                <h2>Météo actuelle</h2>
                <span>Open-Meteo</span>
              </div>
              <div className="ws-panel-body">
                <div className="ws-row" style={{ gridTemplateColumns: 'repeat(4, minmax(0,1fr))' }}>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.temperature ?? '—'}°C</strong><small>Température</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.humidity ?? '—'}%</strong><small>Humidité</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.wind_speed ?? '—'} km/h</strong><small>Vent</small></div>
                  <div className="ws-row-detail" style={{ fontSize: '13px' }}><strong>{weather.weather_condition ?? '—'}</strong><small>Condition</small></div>
                </div>
              </div>
            </div>
          )}

          {/* ===== Contenu principal ===== */}
          <div className="ws-dashboard-grid" style={{ marginTop: '22px' }}>
            <div className="ws-stack">
              {recLoading ? (
                <Spinner label="Recommandations…" />
              ) : recommendations && recommendations.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Recommandations</h2>
                    <span>Priorisées</span>
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
                <EmptyState title="Aucune recommandation" description="Aucune recommandation à afficher pour le moment." />
              )}

              {activitiesLoading ? (
                <Spinner label="Activités…" />
              ) : activities && activities.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Activités récentes</h2>
                    <span>Historique</span>
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
                <EmptyState title="Aucune activité" description="Les consommations enregistrées apparaîtront ici." />
              )}
            </div>
<div className="ws-stack">
              {!droughtLoading && drought && (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Prédiction de sécheresse</h2>
                    <span>Prévision</span>
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

              {alertsLoading ? (
                <Spinner label="Alertes…" />
              ) : alerts && alerts.length > 0 ? (
                <div className="ws-panel">
                  <div className="ws-panel-header">
                    <h2>Alertes récentes</h2>
                    <span>Non lues</span>
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
                <EmptyState title="Aucune alerte" description="Tout est nominal." />
              )}

              <div className="ws-panel">
                <div className="ws-panel-header">
                  <h2>Actions</h2>
                </div>
                <div className="ws-panel-body">
                  <div className="ws-quick-actions">
                    <button className="ws-action-btn" onClick={() => onNavigate('irrigation')}>Gerer l irrigation</button>
                    <button className="ws-action-btn secondary" onClick={() => onNavigate('consumption')}>Voir la consommation</button>
                    <button className="ws-action-btn secondary" onClick={() => onNavigate('quality')}>Voir la qualité de l'eau</button>
                    <button className="ws-action-btn warning" onClick={() => notify('Voir la vue Sécheresse pour le plan.')}>Plan de sécheresse</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : (
        <EmptyState title="Aucune donnée" description="Ajoutez des données pour visualiser le tableau de bord." />
      )}
    </>
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
