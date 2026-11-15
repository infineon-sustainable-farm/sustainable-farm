/** Activities a consumption can be attributed to (backend ConsumptionActivity), with their labels. */
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
