// Helpers shared by the washing & sorting and drying screens: datetime-local conversion, the
// equipment and operator labels of a record, and the equipment status shown in the form options.
import { formatEnum, notAvailable } from '../utils/format';

/** Convert an API LocalDateTime string to a datetime-local input value. */
export const toDateTimeLocal = (value) => {
  if (!value) return '';
  return String(value).slice(0, 16).replace(' ', 'T');
};

/** Convert a datetime-local input value to the API LocalDateTime format. */
export const fromDateTimeLocal = (value) => {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
};

/** Name of the record's equipment, its code when it is not in the list, N/A without one. */
export const equipmentLabelOf = (equipment, equipmentId) =>
  equipment.find((item) => item.equipmentId === equipmentId)?.equipmentName || equipmentId || notAvailable();

/** Name of the record's operator, its id when it is not in the list, N/A without one. */
export const operatorLabelOf = (operators, operatorId) =>
  operators.find((item) => item.operatorId === operatorId)?.operatorName || operatorId || notAvailable();

/** Translated maintenance status of an equipment, ACTIVE when the register gives none. */
export const equipmentStatusLabel = (status) => formatEnum('washSortEquipmentStatus', status || 'ACTIVE');

/** Filters of the process screens: by batch, by equipment, by start date. */
export const filterByProcess = (items, filters) =>
  items
    .filter((item) => {
      if (filters.batchId !== 'all' && item.batchId !== filters.batchId) return false;
      if (filters.equipmentId !== 'all' && item.equipmentId !== filters.equipmentId) return false;
      if (filters.date && String(item.startTime || '').slice(0, 10) !== filters.date) return false;
      return true;
    })
    .sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || '')));

export const EMPTY_PROCESS_FILTERS = { batchId: 'all', equipmentId: 'all', date: '' };
