import i18n from '../../../shared/i18n/i18n';
import { formatEnum, formatIsoDate } from '../utils/format';

// Values accepted by /api/compliance-records for the type and the result.
export const COMPLIANCE_TYPES = ['HACCP', 'FOOD_SAFETY', 'EU_EXPORT', 'HYGIENE', 'TRACEABILITY'];
export const COMPLIANCE_RESULTS = ['COMPLIANT', 'NON_COMPLIANT', 'PENDING'];

const t = (key, options) => i18n.t(key, { ns: 'producttransformation', ...options });

/** Dash shown in the compliance screens for a missing value. */
export const complianceNone = () => t('compliance.none');

/** Translated label of a compliance type ("Food Safety"). */
export const complianceTypeLabel = (value) => (value ? formatEnum('complianceType', value) : complianceNone());

/** Translated label of a compliance result ("Non-compliant"). */
export const complianceResultLabel = (value) =>
  value ? formatEnum('complianceResult', value) : complianceNone();

/** Next audit date, or the dash when none is planned. */
export const nextAuditLabel = (value) => (value ? formatIsoDate(value) : complianceNone());

const escapeCsv = (value) => {
  if (value === null || value === undefined) return '';
  const text = String(value);
  return `"${text.replace(/"/g, '""')}"`;
};

/** CSV export of the records shown, with translated headers; codes and dates stay as stored. */
export function buildComplianceCsv(records, auditorName) {
  const yesNo = (value) => (value ? t('compliance.csv.yes') : t('compliance.csv.no'));
  const columns = [
    'recordId',
    'batchId',
    'complianceType',
    'requirement',
    'result',
    'evidence',
    'auditor',
    'auditDate',
    'nextAuditDate',
    'compliant',
    'auditOverdue',
  ].map((key) => t(`compliance.csv.${key}`));
  const lines = [columns.map(escapeCsv).join(',')];
  records.forEach((record) => {
    lines.push(
      [
        record.recordId,
        record.batchId,
        record.complianceType,
        record.requirement,
        record.result,
        record.evidence,
        auditorName(record.auditorId),
        record.auditDate,
        record.nextAuditDate,
        yesNo(record.compliant),
        yesNo(record.auditOverdue),
      ]
        .map(escapeCsv)
        .join(','),
    );
  });
  return lines.join('\n');
}

/** Downloads the CSV text as compliance-records.csv. */
export function downloadCsv(text) {
  const link = document.createElement('a');
  link.href = `data:text/csv;charset=utf-8,${encodeURIComponent(text)}`;
  link.download = 'compliance-records.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
