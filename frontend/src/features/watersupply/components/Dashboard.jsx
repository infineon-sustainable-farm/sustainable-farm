import { useKpis, useAlerts, useActivities, useRecommendations, useDroughtPrediction, useWeather, useHealthCheck } from '../hooks/useDashboard'

/**
 * Dashboard principal du module watersupply.
 * Affiche les KPIs, alertes, activités, recommandations IA et météo.
 */
export function Dashboard() {
  const { data: kpis, loading: kpisLoading } = useKpis()
  const { data: alerts, loading: alertsLoading } = useAlerts()
  const { data: activities, loading: activitiesLoading } = useActivities()
  const { data: recommendations, loading: recLoading } = useRecommendations()
  const { data: drought, loading: droughtLoading } = useDroughtPrediction()
  const { data: weather, loading: weatherLoading } = useWeather()
  const { healthy: backendHealthy, loading: healthLoading } = useHealthCheck()

  if (kpisLoading || alertsLoading || activitiesLoading) {
    return <div className="p-6">Chargement du dashboard...</div>
  }

  return (
    <div className="p-6 space-y-6">
      {/* Status du backend */}
      <div className="flex items-center gap-2 text-sm">
        <span className={`w-2 h-2 rounded-full ${backendHealthy ? 'bg-green-500' : 'bg-red-500'}`}></span>
        <span>{healthLoading ? 'Vérification...' : backendHealthy ? 'Backend connecté' : 'Backend déconnecté'}</span>
      </div>

      {/* KPIs */}
      {kpis && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard label="Consommation quotidienne (L)" value={kpis.daily_consumption_liters} />
          <KpiCard label="Consommation mensuelle (L)" value={kpis.monthly_consumption_liters} />
          <KpiCard label="Niveau des réservoirs (%)" value={kpis.tank_level_percentage} />
          <KpiCard label="Économie d'eau (%)" value={kpis.water_savings_percentage} />
          <KpiCard label="Irrigations aujourd'hui" value={kpis.irrigation_count_today} />
          <KpiCard label="Anomalies" value={kpis.anomaly_count} />
          <KpiCard label="Qualité de l'eau" value={kpis.water_quality_status} />
          <KpiCard label="Disponibilité capteurs (%)" value={kpis.sensor_availability_percentage} />
        </div>
      )}

      {/* Météo */}
      {weather && (
        <div className="bg-white p-4 rounded-lg shadow">
          <h3 className="font-semibold mb-2">Météo actuelle</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>Température: {weather.temperature}°C</div>
            <div>Humidité: {weather.humidity}%</div>
            <div>Vent: {weather.wind_speed} km/h</div>
            <div>Condition: {weather.weather_condition}</div>
          </div>
        </div>
      )}

      {/* Recommandations IA */}
      {!recLoading && recommendations && recommendations.length > 0 && (
        <div className="bg-white p-4 rounded-lg shadow">
          <h3 className="font-semibold mb-2">Recommandations IA</h3>
          <ul className="space-y-2">
            {recommendations.map((rec, i) => (
              <li key={i} className="text-sm">
                <span className={`font-medium ${priorityColor(rec.priority)}`}>
                  [{rec.priority}]
                </span>{' '}
                {rec.recommendation}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Prédiction de sécheresse */}
      {!droughtLoading && drought && (
        <div className="bg-white p-4 rounded-lg shadow">
          <h3 className="font-semibold mb-2">Prédiction de sécheresse</h3>
          <p className="text-sm">
            Risque: <span className={`font-medium ${riskColor(drought.risk_level)}`}>{drought.risk_level}</span>
          </p>
          <p className="text-sm mt-1">{drought.recommendations}</p>
        </div>
      )}

      {/* Alertes */}
      {!alertsLoading && alerts && alerts.length > 0 && (
        <div className="bg-white p-4 rounded-lg shadow">
          <h3 className="font-semibold mb-2">Alertes récentes</h3>
          <ul className="space-y-2">
            {alerts.map((alert, i) => (
              <li key={i} className="text-sm">
                <span className="font-medium">{alert.title}</span>: {alert.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Activités */}
      {!activitiesLoading && activities && activities.length > 0 && (
        <div className="bg-white p-4 rounded-lg shadow">
          <h3 className="font-semibold mb-2">Activités récentes</h3>
          <ul className="space-y-2">
            {activities.map((activity, i) => (
              <li key={i} className="text-sm">
                {activity.message} - {activity.timestamp}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function KpiCard({ label, value }) {
  return (
    <div className="bg-white p-4 rounded-lg shadow text-center">
      <div className="text-2xl font-bold">{value}</div>
      <div className="text-xs text-gray-600">{label}</div>
    </div>
  )
}

function priorityColor(priority) {
  switch (priority) {
    case 'critical':
      return 'text-red-600'
    case 'high':
      return 'text-orange-600'
    case 'medium':
      return 'text-yellow-600'
    case 'low':
      return 'text-green-600'
    default:
      return 'text-blue-600'
  }
}

function riskColor(risk) {
  switch (risk) {
    case 'CRITICAL':
      return 'text-red-600'
    case 'HIGH':
      return 'text-orange-600'
    case 'MEDIUM':
      return 'text-yellow-600'
    case 'LOW':
      return 'text-green-600'
    default:
      return 'text-gray-600'
  }
}
