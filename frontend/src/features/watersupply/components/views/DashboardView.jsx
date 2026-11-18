import {
  Activity,
  BatteryMedium,
  CalendarDays,
  Cpu,
  Droplet,
  FlaskConical,
  PiggyBank,
  TriangleAlert,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, KpiCard } from '../../../../shared/ui'
import {
  useKpis,
  useAlerts,
  useActivities,
  useRecommendations,
  useDroughtPrediction,
  useWeather,
  useHealthCheck,
  useWaterBalance,
} from '../../hooks/useDashboard'
import { useWaterFormat } from '../../format'
import { Screen } from '../common/Screen'
import { LoadingState } from '../common/LoadingState'
import { SummaryItem, SummaryList } from '../common/SummaryList'
import { toneOf } from '../common/tones'

function recPriorityDot(p) {
  switch (p) {
    case 'critical': return 'red'
    case 'high': case 'medium': return 'orange'
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

/** Panel title with a muted note on the right, as the former panel headers. */
function Note({ children }) {
  return <span className="text-sm text-text-muted">{children}</span>
}

/**
 * Dashboard connected to the API through the useDashboard hooks. Handles loading, error and
 * empty states.
 */
export function DashboardView({ onNavigate, notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { data: kpis, loading: kpisLoading, error: kpisError } = useKpis()
  const { data: alerts, loading: alertsLoading } = useAlerts()
  const { data: activities, loading: activitiesLoading } = useActivities()
  const { data: recommendations, loading: recLoading } = useRecommendations()
  const { data: drought, loading: droughtLoading } = useDroughtPrediction()
  const { data: weather, loading: weatherLoading } = useWeather()
  const { healthy: backendHealthy, loading: healthLoading } = useHealthCheck()

  const anythingLoading = kpisLoading || alertsLoading || activitiesLoading || weatherLoading
  const healthText = healthLoading
    ? t('dashboard.health.checking')
    : backendHealthy
      ? t('dashboard.health.connected')
      : t('dashboard.health.disconnected')
  const orNone = (value) => value ?? t('common.none')

  function renderContent() {
    if (anythingLoading) return <LoadingState label={t('dashboard.loading')} />
    if (kpisError) {
      return <EmptyState title={t('dashboard.loadErrorTitle')} message={kpisError.message || t('dashboard.loadError')} />
    }
    if (!kpis) return <EmptyState title={t('dashboard.emptyTitle')} message={t('dashboard.empty')} />
    return (
      <>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <KpiCard icon={Droplet} label={t('dashboard.kpis.daily')} value={format.liters(kpis.daily_consumption_liters ?? 0)} />
          <KpiCard icon={CalendarDays} label={t('dashboard.kpis.monthly')} value={format.liters(kpis.monthly_consumption_liters ?? 0)} />
          <KpiCard icon={BatteryMedium} tone="warning" label={t('dashboard.kpis.tankLevel')} value={format.percent(kpis.tank_level_percentage ?? 0)} />
          <KpiCard icon={PiggyBank} label={t('dashboard.kpis.savings')} value={format.percent(kpis.water_savings_percentage ?? 0)} />
          <KpiCard icon={Activity} label={t('dashboard.kpis.irrigations')} value={kpis.irrigation_count_today ?? 0} />
          <KpiCard icon={TriangleAlert} tone="warning" label={t('dashboard.kpis.anomalies')} value={kpis.anomaly_count ?? 0} />
          <KpiCard icon={FlaskConical} label={t('dashboard.kpis.quality')} value={orNone(kpis.water_quality_status)} />
          <KpiCard icon={Cpu} label={t('dashboard.kpis.sensors')} value={format.percent(kpis.sensor_availability_percentage ?? 0)} />
        </div>

        {weather && (
          <Card title={t('dashboard.weather.title')} actions={<Note>{t('dashboard.weather.provider')}</Note>}>
            <dl className="grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
              {[
                [t('dashboard.weather.temperature'), weather.temperature == null ? t('common.none') : t('units.celsius', { value: weather.temperature })],
                [t('dashboard.weather.humidity'), weather.humidity == null ? t('common.none') : format.percent(weather.humidity)],
                [t('dashboard.weather.wind'), weather.wind_speed == null ? t('common.none') : t('units.kmh', { value: weather.wind_speed })],
                [t('dashboard.weather.condition'), orNone(weather.weather_condition)],
              ].map(([label, value]) => (
                <div key={label} className="flex flex-col-reverse">
                  <dt className="mt-1 text-xs font-medium text-text-muted">{label}</dt>
                  <dd className="font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
          </Card>
        )}

        <div className="grid items-start gap-6 lg:grid-cols-5">
          <div className="flex min-w-0 flex-col gap-6 lg:col-span-3">
            {recLoading ? (
              <LoadingState compact label={t('dashboard.recommendations.loading')} />
            ) : recommendations && recommendations.length > 0 ? (
              <Card title={t('dashboard.recommendations.title')} actions={<Note>{t('dashboard.recommendations.note')}</Note>}>
                <SummaryList>
                  {recommendations.map((rec, i) => (
                    <SummaryItem
                      key={i}
                      tone={toneOf(recPriorityDot(rec.priority))}
                      title={<Badge tone={toneOf(recTag(rec.priority))}>{t(`dashboard.priorities.${rec.priority}`, { defaultValue: rec.priority })}</Badge>}
                      detail={rec.recommendation}
                    />
                  ))}
                </SummaryList>
              </Card>
            ) : (
              <EmptyState title={t('dashboard.recommendations.emptyTitle')} message={t('dashboard.recommendations.empty')} />
            )}

            {activitiesLoading ? (
              <LoadingState compact label={t('dashboard.activity.loading')} />
            ) : activities && activities.length > 0 ? (
              <Card title={t('dashboard.activity.title')} actions={<Note>{t('dashboard.activity.note')}</Note>}>
                <SummaryList>
                  {activities.map((a, i) => (
                    <SummaryItem key={i} tone="success" title={a.message} detail={a.timestamp} />
                  ))}
                </SummaryList>
              </Card>
            ) : (
              <EmptyState title={t('dashboard.activity.emptyTitle')} message={t('dashboard.activity.empty')} />
            )}
          </div>

          <div className="flex min-w-0 flex-col gap-6 lg:col-span-2">
            {!droughtLoading && drought && (
              <Card title={t('dashboard.drought.title')} actions={<Note>{t('dashboard.drought.note')}</Note>}>
                <Badge tone={toneOf(droughtRiskTag(drought.risk_level))} className="mb-2">{drought.risk_level}</Badge>
                <p className="text-sm text-text-muted">{drought.recommendations}</p>
              </Card>
            )}

            <WaterBalancePanel />

            {alertsLoading ? (
              <LoadingState compact label={t('dashboard.alerts.loading')} />
            ) : alerts && alerts.length > 0 ? (
              <Card title={t('dashboard.alerts.title')} actions={<Note>{t('dashboard.alerts.note')}</Note>}>
                <SummaryList>
                  {alerts.map((al, i) => (
                    <SummaryItem key={i} tone={toneOf(alertDot(al.type))} title={al.title} detail={al.message} />
                  ))}
                </SummaryList>
              </Card>
            ) : (
              <EmptyState title={t('dashboard.alerts.emptyTitle')} message={t('dashboard.alerts.empty')} />
            )}

            <Card title={t('dashboard.actions.title')}>
              <div className="flex flex-wrap gap-2">
                <Button onClick={() => onNavigate('irrigation')}>{t('dashboard.actions.irrigation')}</Button>
                <Button variant="secondary" onClick={() => onNavigate('consumption')}>{t('dashboard.actions.consumption')}</Button>
                <Button variant="secondary" onClick={() => onNavigate('quality')}>{t('dashboard.actions.quality')}</Button>
                <Button variant="secondary" onClick={() => onNavigate('outages')}>{t('dashboard.actions.outage')}</Button>
                <Button variant="secondary" onClick={() => notify(t('dashboard.actions.droughtPlanNotice'))}>{t('dashboard.actions.droughtPlan')}</Button>
                <Button variant="secondary" onClick={() => notify(t('dashboard.actions.carryOverNotice'))}>{t('dashboard.actions.carryOver')}</Button>
              </div>
            </Card>
          </div>
        </div>
      </>
    )
  }

  return (
    <Screen title={t('dashboard.title')} subtitle={t('dashboard.subtitle')}>
      <p role="status" className="flex items-center gap-2 text-sm text-text-muted">
        <span aria-hidden="true" className={`size-2.5 rounded-pill ${backendHealthy ? 'bg-success-fg' : 'bg-error-fg'}`} />
        <span>{healthText}</span>
      </p>
      {renderContent()}
    </Screen>
  )
}

/**
 * Water balance of the month and what the tanks will hold once the irrigations scheduled for the
 * next 7 days have run (demand_forecast_liters / available_after_forecast_liters).
 */
function WaterBalancePanel() {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { data: balance, loading, error } = useWaterBalance('month')
  if (loading || error || !balance) return null
  const available = balance.available_after_forecast_liters ?? 0
  const status = balance.balance_status === 'deficit' ? 'deficit' : balance.balance_status === 'unknown' ? 'unknown' : 'balanced'
  const statusTone = { deficit: 'error', unknown: 'info', balanced: 'success' }[status]
  return (
    <Card title={t('dashboard.balance.title')} actions={<Badge tone={statusTone}>{t(`dashboard.balance.status.${status}`)}</Badge>}>
      <SummaryList>
        <SummaryItem tone="success" title={t('dashboard.balance.rain')} detail={format.liters(balance.rainwater_in_liters ?? 0)} />
        <SummaryItem tone="warning" title={t('dashboard.balance.irrigation')} detail={format.liters(balance.irrigation_out_liters ?? 0)} />
        <SummaryItem
          tone="success"
          title={t('dashboard.balance.tanks')}
          detail={t('dashboard.balance.tanksDetail', {
            level: format.liters(balance.reserve_level_liters ?? 0),
            capacity: format.liters(balance.reserve_capacity_liters ?? 0),
            pct: format.percent(balance.reserve_level_percentage ?? 0),
          })}
        />
        <SummaryItem
          tone={available < 0 ? 'error' : 'warning'}
          title={t('dashboard.balance.forecast')}
          detail={t('dashboard.balance.forecastDetail', {
            planned: format.liters(balance.demand_forecast_liters ?? 0),
            left: format.liters(available),
          })}
        />
      </SummaryList>
    </Card>
  )
}
