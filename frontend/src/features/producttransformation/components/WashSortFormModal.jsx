import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from '../../../shared/ui';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { AssignmentFields, IdentityFields, NumberField, PeriodFields } from './ProcessFormFields';
import { toDateTimeLocal } from './processRun';

const emptyForm = () => ({
  recordId: '',
  batchId: '',
  inputQuantityKg: '',
  outputQuantityKg: '',
  wasteQuantityKg: '',
  waterUsageLiters: '',
  startTime: '',
  endTime: '',
  equipmentId: '',
  operatorId: '',
});

const toForm = (record) =>
  record
    ? {
        recordId: record.recordId || '',
        batchId: record.batchId || '',
        inputQuantityKg: String(record.inputQuantityKg ?? ''),
        outputQuantityKg: String(record.outputQuantityKg ?? ''),
        wasteQuantityKg: String(record.wasteQuantityKg ?? ''),
        waterUsageLiters: String(record.waterUsageLiters ?? ''),
        startTime: toDateTimeLocal(record.startTime),
        endTime: toDateTimeLocal(record.endTime),
        equipmentId: record.equipmentId || '',
        operatorId: record.operatorId || '',
      }
    : emptyForm();

/**
 * Create or edit a wash & sort record. The record ID and the batch are set at creation and
 * read-only afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function WashSortFormModal({ open, editing, batches, equipment, operators, onClose, onSubmit }) {
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
    if (!editing && !form.recordId.trim()) return t('washSort.validation.recordIdRequired');
    if (!editing && !form.batchId) return t('washSort.validation.batchRequired');
    if (form.inputQuantityKg === '' || Number(form.inputQuantityKg) <= 0) {
      return t('washSort.validation.inputPositive');
    }
    if (form.outputQuantityKg === '' || Number(form.outputQuantityKg) <= 0) {
      return t('washSort.validation.outputPositive');
    }
    if (form.wasteQuantityKg === '' || Number(form.wasteQuantityKg) < 0) {
      return t('washSort.validation.wasteNegative');
    }
    if (form.waterUsageLiters === '' || Number(form.waterUsageLiters) < 0) {
      return t('washSort.validation.waterNegative');
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
      setFormError(err?.message || t('washSort.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('washSort.editTitle') : t('washSort.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <IdentityFields
          idLabel={t('washSort.recordId')}
          idValue={form.recordId}
          onIdChange={update('recordId')}
          batchId={form.batchId}
          onBatchChange={update('batchId')}
          batches={batches}
          editing={Boolean(editing)}
        />

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <NumberField label={t('washSort.inputKg')} min="0.01" value={form.inputQuantityKg} onChange={update('inputQuantityKg')} />
          <NumberField label={t('washSort.outputKg')} min="0.01" value={form.outputQuantityKg} onChange={update('outputQuantityKg')} />
          <NumberField label={t('washSort.wasteKg')} min="0" value={form.wasteQuantityKg} onChange={update('wasteQuantityKg')} />
          <NumberField label={t('washSort.waterL')} min="0" value={form.waterUsageLiters} onChange={update('waterUsageLiters')} />
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
