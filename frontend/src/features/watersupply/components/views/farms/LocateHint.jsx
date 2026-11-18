import { LocateFixed } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '../../../../../shared/ui'
import { useGeoMessage } from './coordinates'

/** Geolocation state under the coordinates: locating, accuracy obtained, or why it failed. */
export function LocateHint({ geo, onLocate }) {
  const { t } = useTranslation('watersupply')
  const geoMessage = useGeoMessage()
  return (
    <div className="flex flex-wrap items-center gap-3 text-xs text-text-muted">
      <Button size="sm" variant="secondary" icon={LocateFixed} onClick={onLocate} disabled={geo.locating}>
        {geo.locating ? t('farms.locating') : t('farms.useMyPosition')}
      </Button>
      {geo.error ? (
        <span className="text-error-fg">{geoMessage(geo.error)}</span>
      ) : geo.accuracy != null ? (
        <span>{t('farms.accuracy', { accuracy: geo.accuracy })}</span>
      ) : null}
    </div>
  )
}
