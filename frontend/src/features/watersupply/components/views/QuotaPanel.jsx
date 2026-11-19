import { useMemo, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Card, EmptyState, ErrorState, IconButton, Input, Select } from '../../../../shared/ui'
import { useFarms, useZones } from '../../hooks/useFarms'
import { useQuotaUsage } from '../../hooks/useQuotas'
import { waterQuotaApi } from '../../api/watersupplyApi'
import { useWaterFormat } from '../../format'
import { WsBulletChart } from '../charts'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'

/** Label key and tone of a quota status (ok / warning 80% / exceeded 100%). */
function statusTag(status) {
  switch (status) {
    case 'exceeded': return { key: 'exceeded', tone: 'error' }
    case 'warning': return { key: 'warning', tone: 'warning' }
    case 'ok': return { key: 'ok', tone: 'success' }
    default: return { key: 'none', tone: 'info' }
  }
}

/** Current month in the format expected by the API (first day of the month, YYYY-MM-01). */
function currentMonthValue() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}-01`
}

/**
 * Monthly water quotas (P8): "target vs actual" tracking per farm or per zone,
 * with visual thresholds at 80% and 100%. The backend also generates an automatic
 * notification (warning then critical) on each threshold crossing.
 */
export function QuotaPanel({ notify }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const { usage, loading, error, refetch } = useQuotaUsage()
  const { farms } = useFarms()
  const { zones } = useZones()
  const [targetType, setTargetType] = useState('farm')
  const [targetId, setTargetId] = useState('')
  const [quotaMonth, setQuotaMonth] = useState(currentMonthValue)
  const [quotaLiters, setQuotaLiters] = useState('')
  const [formError, setFormError] = useState(null)
  const [busy, setBusy] = useState(false)

  const targets = useMemo(() => (targetType === 'farm' ? farms : zones), [targetType, farms, zones])

  const monthLabel = useMemo(() => {
    const [year, month] = currentMonthValue().split('-')
    return format.monthYear(year, month)
  }, [format])

  const submit = (event) => {
    event.preventDefault()
    setFormError(null)
    if (!targetId) {
      setFormError(targetType === 'farm' ? t('quota.errors.chooseFarm') : t('quota.errors.chooseZone'))
      return
    }
    const liters = Number(quotaLiters)
    if (!liters || liters <= 0) {
      setFormError(t('quota.errors.positive'))
      return
    }
    setBusy(true)
    waterQuotaApi
      .createQuota({ targetType, targetId, quotaMonth, quotaLiters: liters })
      .then(() => {
        setQuotaLiters('')
        setTargetId('')
        notify?.(t('quota.saved'))
        return refetch()
      })
      .catch((err) => setFormError(err.message || t('quota.errors.save')))
      .finally(() => setBusy(false))
  }

  const remove = (quotaId) => {
    waterQuotaApi
      .deleteQuota(quotaId)
      .then(() => {
        notify?.(t('quota.deleted'))
        return refetch()
      })
      .catch((err) => setFormError(err.message || t('quota.errors.delete')))
  }

  function usageLine(item) {
    const used = t('quota.used', { pct: format.percent(Math.round(Number(item.usage_percentage) || 0)) })
    const remaining = Number(item.remaining_liters)
    const rest = remaining < 0
      ? t('quota.over', { volume: format.liters(Math.abs(Math.round(remaining))) })
      : t('quota.left', { volume: format.liters(Math.round(remaining || 0)) })
    return t('quota.usageLine', { used, rest })
  }

  function renderList() {
    if (loading) return <LoadingState compact label={t('quota.loading')} />
    if (error) return <ErrorState message={error.message || t('quota.loadError')} />
    if (usage.length === 0) return <EmptyState title={t('quota.emptyTitle')} message={t('quota.empty')} />
    return (
      <ul className="flex flex-col gap-4">
        {usage.map((item) => {
          const tag = statusTag(item.status)
          const quota = Number(item.quota_liters) || 0
          const scale = quota > 0 ? quota * 1.25 : Math.max(Number(item.used_liters) || 0, 1)
          return (
            <li key={item.quota_id} className="rounded-tile border border-border-subtle p-4">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-semibold">
                  {item.target_name || t('quota.unknownTarget')}
                  <span className="ml-2 font-normal text-text-muted">
                    {item.target_type === 'zone' ? t('quota.targetTypes.zone') : t('quota.targetTypes.farm')}
                  </span>
                </p>
                <div className="flex items-center gap-2">
                  <Badge tone={tag.tone}>{t(`quota.status.${tag.key}`)}</Badge>
                  <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => remove(item.quota_id)} />
                </div>
              </div>
              <WsBulletChart
                value={item.used_liters}
                target={quota}
                max={scale}
                zones={[
                  { upTo: quota * 0.8, tone: 'success' },
                  { upTo: quota, tone: 'warning' },
                  { upTo: scale, tone: 'error' },
                ]}
              />
              <p className="mt-2 text-xs text-text-muted">{usageLine(item)}</p>
            </li>
          )
        })}
      </ul>
    )
  }

  return (
    <Card title={t('quota.title')} actions={<span className="text-sm text-text-muted">{monthLabel}</span>}>
      <form className="mb-4 flex flex-wrap items-end gap-3" onSubmit={submit}>
        <Select
          aria-label={t('quota.fields.targetType')}
          className="w-auto"
          value={targetType}
          onChange={(event) => {
            setTargetType(event.target.value)
            setTargetId('')
          }}
        >
          <option value="farm">{t('quota.targetTypes.farm')}</option>
          <option value="zone">{t('quota.targetTypes.zone')}</option>
        </Select>
        <Select aria-label={t('quota.fields.target')} className="w-auto" value={targetId} onChange={(event) => setTargetId(event.target.value)}>
          <option value="">{targetType === 'farm' ? t('quota.chooseFarm') : t('quota.chooseZone')}</option>
          {targets.map((target) => <option key={target.id} value={target.id}>{target.name}</option>)}
        </Select>
        <Input
          aria-label={t('quota.fields.month')}
          className="w-auto"
          type="month"
          value={quotaMonth.slice(0, 7)}
          onChange={(event) => setQuotaMonth(`${event.target.value}-01`)}
        />
        <Input
          className="w-auto"
          type="number"
          min="1"
          step="1"
          placeholder={t('quota.fields.liters')}
          aria-label={t('quota.fields.liters')}
          value={quotaLiters}
          onChange={(event) => setQuotaLiters(event.target.value)}
        />
        <Button type="submit" loading={busy}>
          {busy ? t('quota.saving') : t('quota.submit')}
        </Button>
      </form>
      <FormAlert>{formError}</FormAlert>
      {renderList()}
    </Card>
  )
}
