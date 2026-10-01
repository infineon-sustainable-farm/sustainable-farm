/**
 * Chemins du module watersupply, definis en un seul endroit.
 *
 * Le module est monte sous ce prefixe (voir routes.jsx), comme machinery et plants ;
 * le menu (WaterSupplySidebar) et le routeur lisent tous les deux ces valeurs, donc
 * ils ne peuvent plus diverger. Changer de prefixe ne demande qu'une modification ici.
 */
export const WATERSUPPLY_BASE = '/watersupply'

/**
 * Vues du module dans l'ordre du menu : identifiant de vue -> chemin relatif au prefixe.
 * Une chaine vide designe la vue par defaut (route index du module).
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

/** URL absolue d'une vue du module (liens du menu, navigation programmatique). */
export function viewUrl(viewId) {
  const relative = VIEW_PATHS[viewId]
  if (relative === undefined) return WATERSUPPLY_BASE
  return relative === '' ? WATERSUPPLY_BASE : `${WATERSUPPLY_BASE}/${relative}`
}
