import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';
import {
  COMPLIANCE_RESULTS,
  COMPLIANCE_TYPES,
  complianceResultLabel,
  complianceTypeLabel,
} from './complianceOptions';

/** Batch, type, result, auditor, date and overdue filters of the compliance list. */
export default function ComplianceFilters({ filters, onChange, batches, auditors, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={t('common.records', { count })}>
      <Field label={t('compliance.batch')}>
        <Select aria-label={t('compliance.filterBatch')} value={filters.batchId} onChange={update('batchId')}>
          <option value="all">{t('compliance.allBatches')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('compliance.type')}>
        <Select
          aria-label={t('compliance.filterType')}
          value={filters.complianceType}
          onChange={update('complianceType')}
        >
          <option value="all">{t('compliance.allTypes')}</option>
          {COMPLIANCE_TYPES.map((type) => (
            <option key={type} value={type}>
              {complianceTypeLabel(type)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('compliance.result')}>
        <Select aria-label={t('compliance.filterResult')} value={filters.result} onChange={update('result')}>
          <option value="all">{t('compliance.allResults')}</option>
          {COMPLIANCE_RESULTS.map((result) => (
            <option key={result} value={result}>
              {complianceResultLabel(result)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('compliance.auditor')}>
        <Select aria-label={t('compliance.filterAuditor')} value={filters.auditorId} onChange={update('auditorId')}>
          <option value="all">{t('compliance.allAuditors')}</option>
          {auditors.map((operator) => (
            <option key={operator.operatorId} value={operator.operatorId}>
              {operator.operatorName}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('compliance.auditDate')}>
        <Input type="date" aria-label={t('compliance.filterAuditDate')} value={filters.date} onChange={update('date')} />
      </Field>

      <label className="flex items-center gap-2 pb-2 text-sm">
        <input
          type="checkbox"
          className="h-4 w-4 cursor-pointer accent-primary"
          checked={filters.overdueOnly}
          onChange={(event) => onChange('overdueOnly', event.target.checked)}
        />
        {t('compliance.overdueOnly')}
      </label>
    </FilterBar>
  );
}
