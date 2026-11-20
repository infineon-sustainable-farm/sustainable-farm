import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, Select, Textarea } from '../../../shared/ui';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { QC_RESULTS, QC_STAGES, qcResultLabel, qcStageLabel, toDateTimeInput } from './qcOptions';

const emptyForm = () => ({
  checkpointId: '',
  batchId: '',
  stage: 'INTAKE',
  result: 'PENDING',
  defectsCount: '0',
  inspectorId: '',
  checkpointTime: toDateTimeInput(new Date().toISOString()),
  defects: '',
  notes: '',
});

const toForm = (checkpoint) =>
  checkpoint
    ? {
        checkpointId: checkpoint.checkpointId || '',
        batchId: checkpoint.batchId || '',
        stage: checkpoint.stage || 'INTAKE',
        result: checkpoint.result || 'PENDING',
        defectsCount: String(checkpoint.defectsCount ?? 0),
        inspectorId: checkpoint.inspectorId || '',
        checkpointTime: toDateTimeInput(checkpoint.checkpointTime) || emptyForm().checkpointTime,
        defects: checkpoint.defects || '',
        notes: checkpoint.notes || '',
      }
    : emptyForm();

/**
 * Create or edit a QC checkpoint. The checkpoint ID and the batch are set at creation and
 * read-only afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function QcCheckpointFormModal({ open, editing, batches, inspectors, onClose, onSubmit }) {
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
    if (!editing && !form.checkpointId.trim()) return t('qc.validation.checkpointIdRequired');
    if (!editing && !form.batchId) return t('qc.validation.batchRequired');
    if (!form.stage) return t('qc.validation.stageRequired');
    if (!form.result) return t('qc.validation.resultRequired');
    if (form.defectsCount === '' || Number(form.defectsCount) < 0) {
      return t('qc.validation.defectsCountNegative');
    }
    if (!form.checkpointTime) return t('qc.validation.timeRequired');
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
      setFormError(err?.message || t('qc.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('qc.editTitle') : t('qc.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <Field label={t('qc.checkpointId')}>
          <Input value={form.checkpointId} onChange={update('checkpointId')} disabled={Boolean(editing)} />
        </Field>

        <Field label={t('qc.batch')}>
          <Select value={form.batchId} onChange={update('batchId')} disabled={Boolean(editing)}>
            <option value="">{t('qc.selectBatch')}</option>
            {batches.map((batch) => (
              <option key={batch.batchId} value={batch.batchId}>
                {batch.batchId}
              </option>
            ))}
          </Select>
        </Field>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('qc.stage')}>
            <Select value={form.stage} onChange={update('stage')}>
              {QC_STAGES.map((stage) => (
                <option key={stage} value={stage}>
                  {qcStageLabel(stage)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('qc.result')}>
            <Select value={form.result} onChange={update('result')}>
              {QC_RESULTS.map((result) => (
                <option key={result} value={result}>
                  {qcResultLabel(result)}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('qc.defectsCount')}>
            <Input type="number" min="0" value={form.defectsCount} onChange={update('defectsCount')} />
          </Field>
          <Field label={t('qc.checkpointTime')}>
            <Input type="datetime-local" value={form.checkpointTime} onChange={update('checkpointTime')} />
          </Field>
        </div>

        <Field label={t('qc.inspector')}>
          <Select value={form.inspectorId} onChange={update('inspectorId')}>
            <option value="">{t('qc.noInspector')}</option>
            {inspectors.map((operator) => (
              <option key={operator.operatorId} value={operator.operatorId}>
                {operator.operatorName}
              </option>
            ))}
          </Select>
        </Field>

        <Field label={t('qc.defects')}>
          <Textarea value={form.defects} onChange={update('defects')} rows={2} />
        </Field>

        <Field label={t('qc.notes')}>
          <Textarea value={form.notes} onChange={update('notes')} rows={2} />
        </Field>

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
