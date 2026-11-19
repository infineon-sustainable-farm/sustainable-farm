import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, OwnedResourceSelect, Select } from '../../../shared/ui';
import { OWNER_LINKS } from '../../../app/ownerLinks';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { MANGO_VARIETIES, QUALITY_GRADES, varietyLabel } from './rawIntakeOptions';
import { blockLabel, farmIdOf, sameBlockCode, useFarmSite } from './useFarmSite';

const emptyForm = () => ({
  intakeId: '',
  batchId: '',
  sourceFarm: '',
  sourceBlock: '',
  intakeDate: todayIsoDate(),
  receivedQuantityKg: '',
  receivedVariety: 'KEITT',
  receivedGrade: 'A',
  intakeOperator: '',
});

const toForm = (intake) =>
  intake
    ? {
        intakeId: intake.intakeId || '',
        batchId: intake.batchId || '',
        // null: the farm is read back from the saved block once the site lists are loaded.
        sourceFarm: null,
        savedFarm: intake.sourceFarm || '',
        sourceBlock: intake.sourceBlock || '',
        intakeDate: intake.intakeDate ? String(intake.intakeDate).slice(0, 10) : todayIsoDate(),
        receivedQuantityKg: String(intake.receivedQuantityKg ?? ''),
        receivedVariety: intake.receivedVariety || 'KEITT',
        receivedGrade: intake.receivedGrade || 'A',
        intakeOperator: intake.intakeOperator || '',
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

  const site = useFarmSite();
  // The farm and block pickers hold a farm id and a block code (the API takes a farm id or name).
  const farmValue =
    form.sourceFarm ?? farmIdOf(site.farms, site.fields, form.savedFarm, form.sourceBlock);
  const farmFields = site.fields.filter((field) => String(field.farmId) === farmValue);
  const blockValue =
    farmFields.find((field) => sameBlockCode(field.code, form.sourceBlock))?.code ?? '';

  const chooseFarm = (farmId) =>
    setForm((current) => ({
      ...current,
      sourceFarm: farmId,
      sourceBlock: site.fields.some(
        (field) => String(field.farmId) === farmId && sameBlockCode(field.code, current.sourceBlock),
      )
        ? current.sourceBlock
        : '',
    }));
  const chooseBlock = (code) => setForm((current) => ({ ...current, sourceFarm: farmValue, sourceBlock: code }));

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  const close = () => {
    if (saving) return;
    onClose();
  };

  const validate = () => {
    if (!editing && !form.intakeId.trim()) return t('rawIntake.validation.intakeIdRequired');
    if (!editing && !form.batchId) return t('rawIntake.validation.batchRequired');
    if (!form.intakeDate) return t('rawIntake.validation.dateRequired');
    if (!farmValue) return t('rawIntake.validation.sourceFarmRequired');
    if (!blockValue) return t('rawIntake.validation.sourceBlockRequired');
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
      await onSubmit({ ...form, sourceFarm: farmValue, sourceBlock: blockValue });
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
          <OwnedResourceSelect
            label={t('rawIntake.sourceFarm')}
            value={farmValue}
            onChange={chooseFarm}
            options={site.farms}
            loading={site.loading}
            loadError={site.error}
            onRetry={site.reload}
            owner={OWNER_LINKS.farm}
            required
          />
          <OwnedResourceSelect
            label={t('rawIntake.sourceBlock')}
            value={blockValue}
            onChange={chooseBlock}
            options={farmFields}
            getOptionValue={(field) => field.code}
            getOptionLabel={blockLabel}
            loading={site.loading}
            loadError={site.error}
            onRetry={site.reload}
            // Without a farm there is nothing to add yet: the hint asks for the farm first.
            owner={farmValue ? OWNER_LINKS.field : undefined}
            hint={farmValue ? undefined : t('rawIntake.chooseFarmFirst')}
            disabled={!farmValue}
            required
          />
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

        <Field label={t('rawIntake.intakeOperator')}>
          <Input value={form.intakeOperator} onChange={update('intakeOperator')} />
        </Field>

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
