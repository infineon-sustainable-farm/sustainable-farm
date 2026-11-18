import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, Select, Textarea } from '../../../shared/ui';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import {
  COMPLIANCE_RESULTS,
  COMPLIANCE_TYPES,
  complianceResultLabel,
  complianceTypeLabel,
} from './complianceOptions';

const emptyForm = () => ({
  recordId: '',
  batchId: '',
  complianceType: 'HACCP',
  result: 'PENDING',
  requirement: '',
  evidence: '',
  auditorId: '',
  auditDate: todayIsoDate(),
  nextAuditDate: '',
});

const toForm = (record) =>
  record
    ? {
        recordId: record.recordId || '',
        batchId: record.batchId || '',
        complianceType: record.complianceType || 'HACCP',
        result: record.result || 'PENDING',
        requirement: record.requirement || '',
        evidence: record.evidence || '',
        auditorId: record.auditorId || '',
        auditDate: record.auditDate ? String(record.auditDate).slice(0, 10) : todayIsoDate(),
        nextAuditDate: record.nextAuditDate ? String(record.nextAuditDate).slice(0, 10) : '',
      }
    : emptyForm();

/**
 * Create or edit a compliance record. The record ID and the batch are set at creation and
 * read-only afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function ComplianceFormModal({ open, editing, batches, auditors, onClose, onSubmit }) {
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
    if (!editing && !form.recordId.trim()) return t('compliance.validation.recordIdRequired');
    if (!editing && !form.batchId) return t('compliance.validation.batchRequired');
    if (!form.complianceType) return t('compliance.validation.typeRequired');
    if (!form.requirement.trim()) return t('compliance.validation.requirementRequired');
    if (!form.result) return t('compliance.validation.resultRequired');
    if (!form.auditDate) return t('compliance.validation.auditDateRequired');
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
      setFormError(err?.message || t('compliance.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('compliance.editTitle') : t('compliance.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('compliance.recordId')}>
            <Input value={form.recordId} onChange={update('recordId')} disabled={Boolean(editing)} />
          </Field>
          <Field label={t('compliance.batch')}>
            <Select value={form.batchId} onChange={update('batchId')} disabled={Boolean(editing)}>
              <option value="">{t('compliance.selectBatch')}</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('compliance.complianceType')}>
            <Select value={form.complianceType} onChange={update('complianceType')}>
              {COMPLIANCE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {complianceTypeLabel(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('compliance.result')}>
            <Select value={form.result} onChange={update('result')}>
              {COMPLIANCE_RESULTS.map((result) => (
                <option key={result} value={result}>
                  {complianceResultLabel(result)}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <Field label={t('compliance.requirement')}>
          <Textarea value={form.requirement} onChange={update('requirement')} rows={3} />
        </Field>

        <Field label={t('compliance.evidence')}>
          <Textarea value={form.evidence} onChange={update('evidence')} rows={2} />
        </Field>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('compliance.auditDateField')}>
            <Input type="date" value={form.auditDate} onChange={update('auditDate')} />
          </Field>
          <Field label={t('compliance.nextAuditDate')}>
            <Input type="date" value={form.nextAuditDate} onChange={update('nextAuditDate')} />
          </Field>
        </div>

        <Field label={t('compliance.auditor')}>
          <Select value={form.auditorId} onChange={update('auditorId')}>
            <option value="">{t('compliance.noAuditor')}</option>
            {auditors.map((operator) => (
              <option key={operator.operatorId} value={operator.operatorId}>
                {operator.operatorName}
              </option>
            ))}
          </Select>
        </Field>

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
