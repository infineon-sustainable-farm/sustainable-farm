import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';

/** Batch, equipment and start date filters of the washing & sorting and drying screens. */
export default function ProcessFilters({ filters, onChange, batches, equipment, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={count}>
      <Field label={t('washSort.batch')}>
        <Select aria-label={t('washSort.filterBatch')} value={filters.batchId} onChange={update('batchId')}>
          <option value="all">{t('washSort.allBatches')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('washSort.equipment')}>
        <Select
          aria-label={t('washSort.filterEquipment')}
          value={filters.equipmentId}
          onChange={update('equipmentId')}
        >
          <option value="all">{t('washSort.allEquipment')}</option>
          {equipment.map((item) => (
            <option key={item.equipmentId} value={item.equipmentId}>
              {item.equipmentName}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('washSort.startDate')}>
        <Input
          type="date"
          aria-label={t('washSort.filterStartDate')}
          value={filters.date}
          onChange={update('date')}
        />
      </Field>
    </FilterBar>
  );
}
