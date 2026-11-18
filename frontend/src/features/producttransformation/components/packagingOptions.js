import i18n from '../../../shared/i18n/i18n';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';

// Package types accepted by /api/packaging-records.
export const PACKAGE_TYPES = ['ONE_KG_BAG', 'TWO_KG_BAG', 'BULK'];

/** Translated label of a package type; an unknown code keeps a readable label. */
export const packageLabel = (value) => {
  if (!value) return i18n.t('producttransformation:packaging.noValue');
  return i18n.t(`producttransformation:enums.packagingType.${value}`, { defaultValue: formatEnumLabel(value) });
};

/**
 * Suggest an editable, traceability-friendly lot code.
 * Uniqueness is still enforced by the backend, so this only fills the field.
 */
export const suggestLotCode = (batchId, packagingDate) => {
  const now = new Date();
  const time = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(
    2,
    '0',
  )}${String(now.getSeconds()).padStart(2, '0')}`;
  const date = String(packagingDate || todayIsoDate()).replace(/-/g, '');
  return `LOT-${(batchId || 'BATCH').toUpperCase()}-${date}-${time}`;
};
