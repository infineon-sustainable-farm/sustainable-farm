import i18n from '../../../shared/i18n/i18n';
import { formatEnum, notAvailable } from '../utils/format';

// Values accepted by /api/product-transformation/equipment for the PT attributes of a machine.
export const EQUIPMENT_TYPES = ['WASHING', 'DRYING', 'PACKAGING', 'TESTA_DRYER', 'SOLAR_DRYER', 'TUNNEL_DRYER'];
export const MAINTENANCE_STATUSES = ['ACTIVE', 'MAINTENANCE', 'RETIRED'];

/** Same maximum as the Machinery equipment form: one register, one name length. */
export const EQUIPMENT_NAME_MAX_LENGTH = 100;

/**
 * Translated label of an equipment type. A machine created in Machinery has no type yet and shows
 * N/A; an unknown code is shown as received.
 */
export const equipmentTypeLabel = (value) => {
  if (!value) return notAvailable();
  return i18n.t(`producttransformation:enums.equipmentType.${value}`, { defaultValue: value });
};

/** Translated label of a maintenance status, "Unknown" when the machine has none. */
export const maintenanceStatusLabel = (value) => formatEnum('equipmentStatus', value || 'UNKNOWN');

/** Badge tone of a maintenance status. */
export const maintenanceStatusTone = (value) =>
  ({ ACTIVE: 'success', MAINTENANCE: 'warning', RETIRED: 'neutral' })[value] ?? 'neutral';
