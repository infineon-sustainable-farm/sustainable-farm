import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';
import { PACKAGE_TYPES, packageLabel } from './packagingOptions';

/** Batch, package type, export readiness and date filters of the packaging list. */
export default function PackagingFilters({ filters, onChange, batches, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={t('common.records', { count })}>
      <Field label={t('packaging.batch')}>
        <Select aria-label={t('packaging.filterBatch')} value={filters.batchId} onChange={update('batchId')}>
          <option value="all">{t('packaging.allBatches')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('packaging.packageType')}>
        <Select
          aria-label={t('packaging.filterPackageType')}
          value={filters.packageType}
          onChange={update('packageType')}
        >
          <option value="all">{t('packaging.allTypes')}</option>
          {PACKAGE_TYPES.map((type) => (
            <option key={type} value={type}>
              {packageLabel(type)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('packaging.export')}>
        <Select
          aria-label={t('packaging.filterExport')}
          value={filters.exportReady}
          onChange={update('exportReady')}
        >
          <option value="all">{t('packaging.allRecords')}</option>
          <option value="yes">{t('packaging.exportReady')}</option>
          <option value="no">{t('packaging.notExportReady')}</option>
        </Select>
      </Field>

      <Field label={t('packaging.packagingDate')}>
        <Input type="date" aria-label={t('packaging.filterDate')} value={filters.date} onChange={update('date')} />
      </Field>
    </FilterBar>
  );
}
