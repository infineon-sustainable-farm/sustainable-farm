import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';
import { MANGO_VARIETIES, QUALITY_GRADES, varietyLabel } from './rawIntakeOptions';

/** Batch, variety, grade and date filters of the raw intake list. */
export default function RawIntakeFilters({ filters, onChange, batches, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={t('common.records', { count })}>
      <Field label={t('rawIntake.batch')}>
        <Select aria-label={t('rawIntake.filterBatch')} value={filters.batchId} onChange={update('batchId')}>
          <option value="all">{t('rawIntake.allBatches')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('rawIntake.variety')}>
        <Select aria-label={t('rawIntake.filterVariety')} value={filters.variety} onChange={update('variety')}>
          <option value="all">{t('rawIntake.allVarieties')}</option>
          {MANGO_VARIETIES.map((variety) => (
            <option key={variety} value={variety}>
              {varietyLabel(variety)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('rawIntake.grade')}>
        <Select aria-label={t('rawIntake.filterGrade')} value={filters.grade} onChange={update('grade')}>
          <option value="all">{t('rawIntake.allGrades')}</option>
          {QUALITY_GRADES.map((grade) => (
            <option key={grade} value={grade}>
              {t('enums.grade', { grade })}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('rawIntake.intakeDate')}>
        <Input
          type="date"
          aria-label={t('rawIntake.filterDate')}
          value={filters.date}
          onChange={update('date')}
        />
      </Field>
    </FilterBar>
  );
}
