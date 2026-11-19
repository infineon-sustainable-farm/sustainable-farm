import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, ErrorState } from '../../../../shared/ui'
import { useDroughtPrediction } from '../../hooks/useDashboard'
import { useWaterFormat } from '../../format'
import { WsRadialGauge } from '../charts'
import { C } from '../chartUtils'
import { Screen, SplitLayout } from '../common/Screen'
import { LoadingState } from '../common/LoadingState'

function riskMeta(risk) {
  switch ((risk || '').toUpperCase()) {
    case 'CRITICAL': return { step: 4, tone: 'error', level: 'critical' }
    case 'HIGH': return { step: 3, tone: 'error', level: 'alert' }
    case 'MEDIUM': return { step: 2, tone: 'warning', level: 'vigilance' }
    default: return { step: 1, tone: 'success', level: 'normal' }
  }
}

const STEPS = ['normal', 'vigilance', 'alert', 'critical']
// Colours of the active step, from the tone tokens; the step name carries the level.
const STEP_ACTIVE = ['bg-success-bg text-success-fg', 'bg-primary-tint text-text-accent', 'bg-warning-bg text-warning-fg', 'bg-error-bg text-error-fg']

/** Small figure of the recommendation panel. */
function Figure({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-muted">{label}</p>
      <p className="font-semibold">{value}</p>
    </div>
  )
}

export function DroughtView({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { data, loading, error } = useDroughtPrediction()
  const meta = riskMeta(data?.risk_level)
  const levelText = t(`drought.levels.${meta.level}`)

  function renderContent() {
    if (loading) return <LoadingState label={t('common.loading')} />
    if (error) return <ErrorState message={error.message || t('drought.loadError')} />
    if (!data) return <EmptyState message={t('drought.empty')} />
    const gaugeColor = meta.step >= 3 ? C.red : meta.step === 2 ? C.orange : C.green
    return (
      <SplitLayout
        main={
          <Card title={t('drought.gaugeTitle')} actions={<Badge tone={meta.tone}>{levelText}</Badge>}>
            <WsRadialGauge value={data.reservoir_level_percentage ?? 0} color={gaugeColor} />
            <ol className="mt-3 grid grid-cols-4 gap-2" aria-label={t('drought.stepsLabel')}>
              {STEPS.map((step, i) => {
                const active = meta.step === i + 1
                return (
                  <li
                    key={step}
                    aria-current={active ? 'step' : undefined}
                    className={`grid min-h-11 place-items-center rounded-control text-xs font-bold ${active ? STEP_ACTIVE[i] : 'bg-neutral-bg text-text-muted'}`}
                  >
                    {t(`drought.levels.${step}`)}
                  </li>
                )
              })}
            </ol>
          </Card>
        }
        aside={
          <>
            <Card title={t('drought.recommendationTitle')}>
              <Badge tone={meta.tone}>{levelText}</Badge>
              <div className="mt-3 mb-1 flex flex-wrap gap-6 text-sm">
                <Figure
                  label={t('drought.reserveDays')}
                  value={data.days_of_reserve_remaining != null
                    ? t('drought.days', { value: format.number(Math.round(data.days_of_reserve_remaining * 10) / 10) })
                    : t('drought.notAvailable')}
                />
                <Figure
                  label={t('drought.averageConsumption')}
                  value={t('drought.litersPerDay', { value: format.number(data.daily_average_consumption_liters_7d ?? 0) })}
                />
                <Figure
                  label={t('drought.reserve')}
                  value={t('drought.reserveValue', {
                    reserve: format.number(data.total_reserve_liters ?? 0),
                    capacity: format.number(data.total_capacity_liters ?? 0),
                  })}
                />
              </div>
              <p className="mt-2 text-sm text-text-muted">{data.recommendations || t('drought.defaultRecommendation')}</p>
              <p className="mt-2 text-xs text-text-muted">
                {t('drought.prediction', {
                  date: data.prediction_date ? format.dateTime(data.prediction_date) : t('common.none'),
                  confidence: format.percent((data.confidence ?? 0) * 100),
                })}
              </p>
            </Card>
            <Card title={t('drought.automationTitle')}>
              <Badge tone="error">{t('drought.ifCritical')}</Badge>
              <p className="my-3 text-sm text-text-muted">{t('drought.automationText')}</p>
              <Button size="sm" onClick={() => notify(t('drought.planPrepared'))}>{t('drought.preparePlan')}</Button>
            </Card>
          </>
        }
      />
    )
  }

  return (
    <Screen title={t('drought.title')} subtitle={t('drought.subtitle')}>
      {renderContent()}
    </Screen>
  )
}
