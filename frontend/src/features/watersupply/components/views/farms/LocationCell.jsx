import { useTranslation } from 'react-i18next'
import { mapUrl } from '../../../hooks/useGeolocation'
import { useWaterFormat } from '../../../format'

/** Position cell of the tables: coordinates with a link to check them on a map. */
export function LocationCell({ latitude, longitude }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  if (latitude == null || longitude == null || latitude === '' || longitude === '') return format.none
  return (
    <a
      href={mapUrl(latitude, longitude)}
      target="_blank"
      rel="noopener noreferrer"
      title={t('farms.openMap')}
      className="text-text-accent underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring"
    >
      {t('farms.coordinates', { latitude: format.fixed(latitude, 4), longitude: format.fixed(longitude, 4) })}
    </a>
  )
}
