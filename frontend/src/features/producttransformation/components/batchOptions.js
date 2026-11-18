import i18n from '../../../shared/i18n/i18n';

// Statuses of a batch along the processing line, as /api/product-transformation/batches sends them.
export const BATCH_STATUSES = [
  'CREATED',
  'INTAKE',
  'WASHING',
  'DRYING',
  'PACKAGING',
  'COMPLETED',
  'SHIPPED',
  'REJECTED',
];

/** Status name of the filter ("Washing"). */
export const batchStatusLabel = (status) =>
  i18n.t(`enums.batchStatus.${status}`, { ns: 'producttransformation', defaultValue: status });

/** Status shown in the badge of a row, the code itself in English ("WASHING"). */
export const batchStatusCode = (status) =>
  i18n.t(`enums.batchStatusCode.${status}`, { ns: 'producttransformation', defaultValue: status ?? '' });

/** Only batches that are neither shipped nor rejected can move to the next status. */
export const canAdvance = (batch) => batch.currentStatus !== 'SHIPPED' && batch.currentStatus !== 'REJECTED';
