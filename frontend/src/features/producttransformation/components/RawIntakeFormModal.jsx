import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, Select } from '../../../shared/ui';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import PersonSelect from '../../../app/PersonSelect';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { MANGO_VARIETIES, QUALITY_GRADES, varietyLabel } from './rawIntakeOptions';

const emptyForm = () => ({
  intakeId: '',
  batchId: '',
  sourceFarm: '',
  sourceBlock: '',
  intakeDate: todayIsoDate(),
  receivedQuantityKg: '',
  receivedVariety: 'KEITT',
  receivedGrade: 'A',
  intakeOperatorPersonId: '',
});

const toForm = (intake) =>
  intake
    ? {
        intakeId: intake.intakeId || '',
        batchId: intake.batchId || '',
        sourceFarm: intake.sourceFarm || '',
        sourceBlock: intake.sourceBlock || '',
        intakeDate: intake.intakeDate ? String(intake.intakeDate).slice(0, 10) : todayIsoDate(),
        receivedQuantityKg: String(intake.receivedQuantityKg ?? ''),
        receivedVariety: intake.receivedVariety || 'KEITT',
        receivedGrade: intake.receivedGrade || 'A',
        intakeOperatorPersonId: intake.intakeOperatorPersonId != null ? String(intake.intakeOperatorPersonId) : '',
      }
    : emptyForm();

/**
 * Create or edit a raw intake. The intake ID and the batch are set at creation and read-only
 * afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function RawIntakeFormModal({ open, editing, batches, onClose, onSubmit }) {
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
    if (!editing && !form.intakeId.trim()) return t('rawIntake.validation.intakeIdRequired');
    if (!editing && !form.batchId) return t('rawIntake.validation.batchRequired');
    if (!form.intakeDate) return t('rawIntake.validation.dateRequired');
    if (!form.sourceFarm.trim()) return t('rawIntake.validation.sourceFarmRequired');
    if (!form.sourceBlock.trim()) return t('rawIntake.validation.sourceBlockRequired');
    if (form.receivedQuantityKg === '' || Number(form.receivedQuantityKg) <= 0) {
      return t('rawIntake.validation.quantityPositive');
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
      setFormError(err?.message || t('rawIntake.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('rawIntake.editTitle') : t('rawIntake.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('rawIntake.intakeId')}>
            <Input value={form.intakeId} onChange={update('intakeId')} disabled={Boolean(editing)} />
          </Field>
          <Field label={t('rawIntake.batch')}>
            <Select value={form.batchId} onChange={update('batchId')} disabled={Boolean(editing)}>
              <option value="">{t('rawIntake.selectBatch')}</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('rawIntake.sourceFarm')}>
            <Input value={form.sourceFarm} onChange={update('sourceFarm')} />
          </Field>
          <Field label={t('rawIntake.sourceBlock')}>
            <Input value={form.sourceBlock} onChange={update('sourceBlock')} />
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('rawIntake.intakeDate')}>
            <Input type="date" value={form.intakeDate} onChange={update('intakeDate')} />
          </Field>
          <Field label={t('rawIntake.quantityKg')}>
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.receivedQuantityKg}
              onChange={update('receivedQuantityKg')}
            />
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('rawIntake.variety')}>
            <Select value={form.receivedVariety} onChange={update('receivedVariety')}>
              {MANGO_VARIETIES.map((variety) => (
                <option key={variety} value={variety}>
                  {varietyLabel(variety)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('rawIntake.grade')}>
            <Select value={form.receivedGrade} onChange={update('receivedGrade')}>
              {QUALITY_GRADES.map((grade) => (
                <option key={grade} value={grade}>
                  {t('enums.grade', { grade })}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <PersonSelect
          label={t('rawIntake.intakeOperator')}
          value={form.intakeOperatorPersonId}
          currentName={editing?.intakeOperator}
          onChange={(id) => setForm((current) => ({ ...current, intakeOperatorPersonId: id }))}
          placeholder={t('rawIntake.noOperator')}
        />

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
