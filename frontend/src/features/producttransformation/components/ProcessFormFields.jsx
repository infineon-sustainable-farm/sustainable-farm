import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import { equipmentStatusLabel } from './processRun';

/**
 * Form fields shared by the washing & sorting and drying forms: the record ID and batch (read-only
 * once created), the start and end times, the equipment and operator.
 */
export function IdentityFields({ idLabel, idValue, onIdChange, batchId, onBatchChange, batches, editing }) {
  const { t } = useTranslation('producttransformation');
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <Field label={idLabel}>
        <Input value={idValue} onChange={onIdChange} disabled={editing} />
      </Field>
      <Field label={t('washSort.batch')}>
        <Select value={batchId} onChange={onBatchChange} disabled={editing}>
          <option value="">{t('washSort.selectBatch')}</option>
          {batches.map((batch) => (
            <option key={batch.batchId} value={batch.batchId}>
              {batch.batchId}
            </option>
          ))}
        </Select>
      </Field>
    </div>
  );
}

export function PeriodFields({ startTime, endTime, onStartChange, onEndChange }) {
  const { t } = useTranslation('producttransformation');
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <Field label={t('washSort.startTime')}>
        <Input type="datetime-local" value={startTime} onChange={onStartChange} />
      </Field>
      <Field label={t('washSort.endTime')}>
        <Input type="datetime-local" value={endTime} onChange={onEndChange} />
      </Field>
    </div>
  );
}

export function AssignmentFields({ equipmentId, operatorId, onEquipmentChange, onOperatorChange, equipment, operators }) {
  const { t } = useTranslation('producttransformation');
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <Field label={t('washSort.equipment')}>
        <Select value={equipmentId} onChange={onEquipmentChange}>
          <option value="">{t('washSort.noEquipment')}</option>
          {equipment.map((item) => (
            <option key={item.equipmentId} value={item.equipmentId}>
              {t('washSort.equipmentOption', {
                name: item.equipmentName,
                status: equipmentStatusLabel(item.maintenanceStatus),
              })}
            </option>
          ))}
        </Select>
      </Field>
      <Field label={t('washSort.operator')}>
        <Select value={operatorId} onChange={onOperatorChange}>
          <option value="">{t('washSort.noOperator')}</option>
          {operators.map((operator) => (
            <option key={operator.operatorId} value={operator.operatorId}>
              {operator.operatorName}
            </option>
          ))}
        </Select>
      </Field>
    </div>
  );
}

/** Number input in a field, as the quantity, temperature and moisture fields of the forms. */
export function NumberField({ label, value, onChange, min, max, step = '0.01' }) {
  return (
    <Field label={label}>
      <Input type="number" min={min} max={max} step={step} value={value} onChange={onChange} />
    </Field>
  );
}
