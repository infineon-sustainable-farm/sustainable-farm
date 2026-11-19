import { useTranslation } from 'react-i18next'

/**
 * Activities a consumption can be attributed to (backend ConsumptionActivity), with their English
 * labels. The screens show the translation `lots.activities.<value>` of the watersupply namespace;
 * activityLabel stays the English fallback for an unknown value.
 */
export const ACTIVITIES = [
  ['WASHING', 'Washing'],
  ['CUTTING', 'Cutting'],
  ['DRYER_CLEANING', 'Dryer cleaning'],
  ['IRRIGATION', 'Irrigation'],
  ['OTHER', 'Other'],
]

export function activityLabel(activity) {
  return ACTIVITIES.find(([key]) => key === activity)?.[1] || activity || 'Other'
}

/** Translated label of an activity for the active language: `label('WASHING')`. */
export function useActivityLabel() {
  const { t } = useTranslation('watersupply')
  return (activity) => t(`lots.activities.${activity || 'OTHER'}`, { defaultValue: activityLabel(activity) })
}
