import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from '../../../shared/ui';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { AssignmentFields, IdentityFields, NumberField, PeriodFields } from './ProcessFormFields';
import { toDateTimeLocal } from './processRun';

// EU compliance range of the end moisture, also enforced by the backend.
const MIN_END_MOISTURE = 6;
const MAX_END_MOISTURE = 18;

const FIELDS = [
  'durationHours',
  'targetTemperatureC',
  'actualTemperatureC',
  'startMoisturePct',
  'endMoisturePct',
  'energyUsageKwh',
];

const toForm = (run) => ({
  runId: run?.runId || '',
  batchId: run?.batchId || '',
  ...Object.fromEntries(FIELDS.map((key) => [key, run ? String(run[key] ?? '') : ''])),
  startTime: run ? toDateTimeLocal(run.startTime) : '',
  endTime: run ? toDateTimeLocal(run.endTime) : '',
  equipmentId: run?.equipmentId || '',
  operatorId: run?.operatorId || '',
});

/**
 * Create or edit a drying run. The run ID and the batch are set at creation and read-only
 * afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function DryingRunFormModal({ open, editing, batches, equipment, operators, onClose, onSubmit }) {
  const { t } = useTranslation('producttransformation');
  const [form, setForm] = useState(() => toForm(editing));
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  const close = () => {
    if (saving) return;
    onClose();
  };

  const validate = () => {
    if (!editing && !form.runId.trim()) return t('drying.validation.runIdRequired');
    if (!editing && !form.batchId) return t('drying.validation.batchRequired');
    if (form.durationHours === '' || Number(form.durationHours) <= 0) {
      return t('drying.validation.durationPositive');
    }
    if (form.targetTemperatureC === '' || Number.isNaN(Number(form.targetTemperatureC))) {
      return t('drying.validation.targetRequired');
    }
    if (form.actualTemperatureC === '' || Number.isNaN(Number(form.actualTemperatureC))) {
      return t('drying.validation.actualRequired');
    }
    if (form.startMoisturePct === '' || Number(form.startMoisturePct) <= 0) {
      return t('drying.validation.startMoisturePositive');
    }
    if (form.endMoisturePct === '') return t('drying.validation.endMoistureRequired');
    const endMoisture = Number(form.endMoisturePct);
    if (endMoisture < MIN_END_MOISTURE || endMoisture > MAX_END_MOISTURE) {
      return t('drying.validation.endMoistureRange', { min: MIN_END_MOISTURE, max: MAX_END_MOISTURE });
    }
    if (form.energyUsageKwh === '' || Number(form.energyUsageKwh) < 0) {
      return t('drying.validation.energyNegative');
    }
    if (!form.startTime) return t('washSort.validation.startRequired');
    if (!form.endTime) return t('washSort.validation.endRequired');
    if (new Date(form.endTime) <= new Date(form.startTime)) {
      return t('washSort.validation.endAfterStart');
    }
    return '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      await onSubmit(form);
    } catch (err) {
      setFormError(err?.message || t('drying.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('drying.editTitle') : t('drying.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <IdentityFields
          idLabel={t('drying.runId')}
          idValue={form.runId}
          onIdChange={update('runId')}
          batchId={form.batchId}
          onBatchChange={update('batchId')}
          batches={batches}
          editing={Boolean(editing)}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <NumberField label={t('drying.durationH')} min="0.01" value={form.durationHours} onChange={update('durationHours')} />
          <NumberField label={t('drying.targetTemp')} step="0.1" value={form.targetTemperatureC} onChange={update('targetTemperatureC')} />
          <NumberField label={t('drying.actualTemp')} step="0.1" value={form.actualTemperatureC} onChange={update('actualTemperatureC')} />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <NumberField label={t('drying.startMoisture')} min="0.01" value={form.startMoisturePct} onChange={update('startMoisturePct')} />
          <NumberField
            label={t('drying.endMoisture')}
            min={MIN_END_MOISTURE}
            max={MAX_END_MOISTURE}
            value={form.endMoisturePct}
            onChange={update('endMoisturePct')}
          />
          <NumberField label={t('drying.energyKwh')} min="0" value={form.energyUsageKwh} onChange={update('energyUsageKwh')} />
        </div>

        <PeriodFields
          startTime={form.startTime}
          endTime={form.endTime}
          onStartChange={update('startTime')}
          onEndChange={update('endTime')}
        />

        <AssignmentFields
          equipmentId={form.equipmentId}
          operatorId={form.operatorId}
          onEquipmentChange={(id) => setForm((current) => ({ ...current, equipmentId: id }))}
          onOperatorChange={update('operatorId')}
          equipment={equipment}
          operators={operators}
        />

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
