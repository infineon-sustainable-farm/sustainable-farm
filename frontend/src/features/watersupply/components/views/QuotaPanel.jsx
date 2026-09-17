import { useMemo, useState } from 'react'
import { useFarms, useZones } from '../../hooks/useFarms'
import { useQuotaUsage } from '../../hooks/useQuotas'
import { waterQuotaApi } from '../../api/watersupplyApi'
import { WsBulletChart } from '../charts'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'

/** Libellé et tonalité d'un statut de quota (ok / warning 80 % / exceeded 100 %). */
function statusTag(status) {
  switch (status) {
    case 'exceeded': return { label: 'Quota dépassé', tone: 'red' }
    case 'warning': return { label: '80 % du quota atteint', tone: 'orange' }
    case 'ok': return { label: 'Sous le quota', tone: 'green' }
    default: return { label: 'Sans référence', tone: 'primary' }
  }
}

/** Mois courant au format attendu par l'API (premier jour du mois, YYYY-MM-01). */
function currentMonthValue() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}-01`
}

/**
 * Quotas mensuels d'eau (P8) : suivi « objectif vs réel » par ferme ou par zone,
 * avec seuils visuels à 80 % et 100 %. Le backend génère en plus une notification
 * automatique (warning puis critical) à chaque franchissement.
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
    return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })
  }, [])

  const submit = (event) => {
    event.preventDefault()
    setFormError(null)
    if (!targetId) {
      setFormError(targetType === 'farm' ? 'Choisissez une ferme.' : 'Choisissez une zone.')
      return
    }
    const liters = Number(quotaLiters)
    if (!liters || liters <= 0) {
      setFormError('Le quota doit être un volume positif (litres).')
      return
    }
    setBusy(true)
    waterQuotaApi
      .createQuota({ targetType, targetId, quotaMonth, quotaLiters: liters })
      .then(() => {
        setQuotaLiters('')
        setTargetId('')
        notify?.('Quota enregistré.')
        return refetch()
      })
      .catch((err) => setFormError(err.message || 'Impossible d enregistrer le quota.'))
      .finally(() => setBusy(false))
  }

  const remove = (quotaId) => {
    waterQuotaApi
      .deleteQuota(quotaId)
      .then(() => {
        notify?.('Quota supprimé.')
        return refetch()
      })
      .catch((err) => setFormError(err.message || 'Impossible de supprimer le quota.'))
  }

  return (
    <div className="ws-panel" style={{ marginTop: '22px' }}>
      <div className="ws-panel-header">
        <h2>Quotas mensuels d&apos;eau</h2>
        <span>{monthLabel}</span>
      </div>
      <div className="ws-panel-body">
        <form className="ws-filters ws-quota-form" onSubmit={submit}>
          <select value={targetType} onChange={(event) => { setTargetType(event.target.value); setTargetId('') }}>
            <option value="farm">Ferme</option>
            <option value="zone">Zone</option>
          </select>
          <select value={targetId} onChange={(event) => setTargetId(event.target.value)}>
            <option value="">{targetType === 'farm' ? 'Choisir une ferme...' : 'Choisir une zone...'}</option>
            {targets.map((target) => <option key={target.id} value={target.id}>{target.name}</option>)}
          </select>
          <input type="month" value={quotaMonth.slice(0, 7)} onChange={(event) => setQuotaMonth(`${event.target.value}-01`)} />
          <input
            type="number"
            min="1"
            step="1"
            placeholder="Quota mensuel (L)"
            value={quotaLiters}
            onChange={(event) => setQuotaLiters(event.target.value)}
          />
          <button className="ws-action-btn" type="submit" disabled={busy}>
            {busy ? 'Enregistrement…' : 'Définir le quota'}
          </button>
        </form>
        {formError && <p className="ws-quota-error" role="alert">{formError}</p>}

        {loading ? (
          <Spinner label="Chargement des quotas..." />
        ) : error ? (
          <EmptyState title="Erreur" description={error.message || 'Impossible de charger les quotas.'} />
        ) : usage.length === 0 ? (
          <EmptyState
            title="Aucun quota défini"
            description="Définissez un quota mensuel par ferme ou par zone : la consommation des capteurs sera comparée à cet objectif, avec alerte automatique à 80 % puis à 100 %."
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
                      {item.target_name || 'Cible inconnue'}
                      <small style={{ marginLeft: '8px', fontWeight: 400, color: 'var(--ws-muted)' }}>
                        {item.target_type === 'zone' ? 'Zone' : 'Ferme'}
                      </small>
                    </strong>
                    <div className="ws-quota-actions">
                      <span className={`ws-tag ${tag.tone}`}>{tag.label}</span>
                      <button
                        type="button"
                        className="ws-quota-delete"
                        title="Supprimer"
                        onClick={() => remove(item.quota_id)}
                      >
                        Supprimer
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
                    {Math.round(Number(item.usage_percentage) || 0)} % du quota utilisé
                    {Number(item.remaining_liters) < 0
                      ? ` — dépassement de ${Math.abs(Math.round(Number(item.remaining_liters)))} L`
                      : ` — reste ${Math.round(Number(item.remaining_liters) || 0)} L`}
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
