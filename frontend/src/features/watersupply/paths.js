/**
 * Paths of the watersupply module, defined in a single place.
 *
 * The module is mounted under this prefix (see routes.jsx), like machinery and plants;
 * the menu (WaterSupplySidebar) and the router both read these values, so they can no
 * longer drift apart. Changing the prefix takes a single edit here.
 */
export const WATERSUPPLY_BASE = '/watersupply'

/**
 * Module views in menu order: view id -> path relative to the prefix.
 * An empty string designates the default view (the module's index route).
 */
export const VIEW_PATHS = {
  dashboard: '',
  farms: 'farms',
  sources: 'sources',
  irrigation: 'irrigation',
  consumption: 'consumption',
  rainwater: 'rainwater',
  maintenance: 'maintenance',
  quality: 'quality',
  drought: 'drought',
  notifications: 'notifications',
}

/** Absolute URL of a module view (menu links, programmatic navigation). */
export function viewUrl(viewId) {
  const relative = VIEW_PATHS[viewId]
  if (relative === undefined) return WATERSUPPLY_BASE
  return relative === '' ? WATERSUPPLY_BASE : `${WATERSUPPLY_BASE}/${relative}`
}
