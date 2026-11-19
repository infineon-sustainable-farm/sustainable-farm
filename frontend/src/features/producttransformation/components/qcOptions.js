import i18n from '../../../shared/i18n/i18n';
import { formatEnum } from '../utils/format';

// Values accepted by /api/product-transformation/qc-checkpoints for the stage and the result.
export const QC_STAGES = ['INTAKE', 'WASHING', 'DRYING', 'COOLING', 'PACKAGING', 'FINAL'];
export const QC_RESULTS = ['PENDING', 'PASS', 'FAIL', 'REWORK'];

/** Dash shown in the QC list for a missing stage, result or inspector. */
export const qcNone = () => i18n.t('producttransformation:qc.none');

/** Translated label of a QC stage. */
export const qcStageLabel = (stage) => (stage ? formatEnum('qcStage', stage) : qcNone());

/** Translated label of a QC result. */
export const qcResultLabel = (result) => (result ? formatEnum('qcResult', result) : qcNone());

/**
 * Convert an ISO/QcCheckpoint timestamp into a `datetime-local` input value.
 * Backend returns LocalDateTime as `2026-11-09T10:30:00`.
 */
export function toDateTimeInput(value) {
  if (!value) return '';
  return String(value).slice(0, 16);
}

/** Compare only the calendar day of two timestamp values. */
export function matchesDate(value, isoDate) {
  if (!isoDate) return true;
  if (!value) return false;
  const day = new Date(value);
  if (Number.isNaN(day.getTime())) return false;
  const local = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(
    day.getDate(),
  ).padStart(2, '0')}`;
  return local === isoDate;
}
