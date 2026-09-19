import { useMemo, useState } from 'react'
import { useFarms, useZones } from '../../hooks/useFarms'
import { useQuotaUsage } from '../../hooks/useQuotas'
import { waterQuotaApi } from '../../api/watersupplyApi'
import { WsBulletChart } from '../charts'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

/** Label and tone of a quota status (ok / warning 80% / exceeded 100%). */
function statusTag(status) {
  switch (status) {
    case 'exceeded': return { label: 'Quota exceeded', tone: 'red' }
    case 'warning': return { label: '80% of quota reached', tone: 'orange' }
    case 'ok': return { label: 'Under quota', tone: 'green' }
    default: return { label: 'No baseline', tone: 'primary' }
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
    return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  }, [])

  const submit = (event) => {
    event.preventDefault()
    setFormError(null)
    if (!targetId) {
      setFormError(targetType === 'farm' ? 'Choose a farm.' : 'Choose a zone.')
      return
    }
    const liters = Number(quotaLiters)
    if (!liters || liters <= 0) {
      setFormError('Quota must be a positive volume (liters).')
      return
    }
    setBusy(true)
    waterQuotaApi
      .createQuota({ targetType, targetId, quotaMonth, quotaLiters: liters })
      .then(() => {
        setQuotaLiters('')
        setTargetId('')
        notify?.('Quota saved.')
        return refetch()
      })
      .catch((err) => setFormError(err.message || 'Unable to save the quota.'))
      .finally(() => setBusy(false))
  }

  const remove = (quotaId) => {
    waterQuotaApi
      .deleteQuota(quotaId)
      .then(() => {
        notify?.('Quota deleted.')
        return refetch()
      })
      .catch((err) => setFormError(err.message || 'Unable to delete the quota.'))
  }

  return (
    <div className="ws-panel" style={{ marginTop: '22px' }}>
      <div className="ws-panel-header">
        <h2>Monthly water quotas</h2>
        <span>{monthLabel}</span>
      </div>
      <div className="ws-panel-body">
        <form className="ws-filters ws-quota-form" onSubmit={submit}>
          <select value={targetType} onChange={(event) => { setTargetType(event.target.value); setTargetId('') }}>
            <option value="farm">Farm</option>
            <option value="zone">Zone</option>
          </select>
          <select value={targetId} onChange={(event) => setTargetId(event.target.value)}>
            <option value="">{targetType === 'farm' ? 'Choose a farm…' : 'Choose a zone…'}</option>
            {targets.map((target) => <option key={target.id} value={target.id}>{target.name}</option>)}
          </select>
          <input type="month" value={quotaMonth.slice(0, 7)} onChange={(event) => setQuotaMonth(`${event.target.value}-01`)} />
          <input
            type="number"
            min="1"
            step="1"
            placeholder="Monthly quota (L)"
            value={quotaLiters}
            onChange={(event) => setQuotaLiters(event.target.value)}
          />
          <button className="ws-action-btn" type="submit" disabled={busy}>
            {busy ? 'Saving…' : 'Set quota'}
          </button>
        </form>
        {formError && <p className="ws-quota-error" role="alert">{formError}</p>}

        {loading ? (
          <Spinner label="Loading quotas…" />
        ) : error ? (
          <EmptyState title="Error" description={error.message || 'Unable to load quotas.'} />
        ) : usage.length === 0 ? (
          <EmptyState
            title="No quota defined"
            description="Set a monthly quota per farm or per zone: sensor consumption will be compared with this target, with an automatic alert at 80% then 100%."
          />
        ) : (
          <div className="ws-quota-list">
            {usage.map((item) => {
              const tag = statusTag(item.status)
              const quota = Number(item.quota_liters) || 0
              const scale = quota > 0 ? quota * 1.25 : Math.max(Number(item.used_liters) || 0, 1)
              return (
                <div className="ws-quota-item" key={item.quota_id}>
                  <div className="ws-quota-head">
                    <strong>
                      {item.target_name || 'Unknown target'}
                      <small style={{ marginLeft: '8px', fontWeight: 400, color: 'var(--ws-muted)' }}>
                        {item.target_type === 'zone' ? 'Zone' : 'Farm'}
                      </small>
                    </strong>
                    <div className="ws-quota-actions">
                      <span className={`ws-tag ${tag.tone}`}>{tag.label}</span>
                      <button
                        type="button"
                        className="ws-quota-delete"
                        title="Delete"
                        onClick={() => remove(item.quota_id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  <WsBulletChart
                    value={item.used_liters}
                    target={quota}
                    max={scale}
                    unit="L"
                    zones={[
                      { upTo: quota * 0.8, color: 'rgba(76,175,80,.25)' },
                      { upTo: quota, color: 'rgba(239,108,0,.25)' },
                      { upTo: scale, color: 'rgba(198,40,40,.25)' },
                    ]}
                  />
                  <p style={{ margin: '8px 0 0', fontSize: '12px', color: 'var(--ws-muted)' }}>
                    {Math.round(Number(item.usage_percentage) || 0)}% of quota used
                    {Number(item.remaining_liters) < 0
                      ? ` — over by ${Math.abs(Math.round(Number(item.remaining_liters)))} L`
                      : ` — ${Math.round(Number(item.remaining_liters) || 0)} L left`}
                  </p>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
