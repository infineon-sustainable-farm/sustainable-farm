import { useState } from 'react';
import { Sparkles } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { OWNER_LINKS } from '../../../app/ownerLinks';
import { Button, Field, Input, Modal, OwnedResourceSelect, Select } from '../../../shared/ui';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { PACKAGE_TYPES, packageLabel, suggestLotCode } from './packagingOptions';

const toForm = (record) => ({
  recordId: record?.recordId || '',
  batchId: record?.batchId || '',
  packageType: record?.packageType || 'ONE_KG_BAG',
  packageQuantityKg: record ? String(record.packageQuantityKg ?? '') : '',
  lotCode: record?.lotCode || '',
  exportReady: Boolean(record?.exportReady),
  packagingDate: record?.packagingDate ? String(record.packagingDate).slice(0, 10) : todayIsoDate(),
  equipmentId: record?.equipmentId || '',
  operatorId: record?.operatorId || '',
});

/**
 * Create or edit a packaging record. The record ID, the batch and the lot code are set at creation
 * and read-only afterwards. `onSubmit(form)` returns a promise; it rejects with the server error.
 */
export default function PackagingFormModal({ open, editing, batches, equipment, operators, onClose, onSubmit }) {
  const { t } = useTranslation('producttransformation');
  const [form, setForm] = useState(() => toForm(editing));
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const update = (key) => (event) => {
    const value = key === 'exportReady' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [key]: value }));
  };

  const close = () => {
    if (saving) return;
    onClose();
  };

  const suggest = () =>
    setForm((current) => ({ ...current, lotCode: suggestLotCode(current.batchId, current.packagingDate) }));

  const validate = () => {
    if (!editing && !form.recordId.trim()) return t('packaging.validation.recordIdRequired');
    if (!editing && !form.batchId) return t('packaging.validation.batchRequired');
    if (!form.packageType) return t('packaging.validation.packageTypeRequired');
    if (form.packageQuantityKg === '' || Number(form.packageQuantityKg) <= 0) {
      return t('packaging.validation.quantityPositive');
    }
    if (!form.lotCode.trim()) return t('packaging.validation.lotCodeRequired');
    if (!form.packagingDate) return t('packaging.validation.dateRequired');
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
      setFormError(err?.message || t('packaging.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('packaging.editTitle') : t('packaging.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('packaging.recordId')}>
            <Input value={form.recordId} onChange={update('recordId')} disabled={Boolean(editing)} />
          </Field>
          <Field label={t('packaging.batch')}>
            <Select value={form.batchId} onChange={update('batchId')} disabled={Boolean(editing)}>
              <option value="">{t('packaging.selectBatch')}</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('packaging.packageType')}>
            <Select value={form.packageType} onChange={update('packageType')}>
              {PACKAGE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {packageLabel(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('packaging.quantityKg')}>
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.packageQuantityKg}
              onChange={update('packageQuantityKg')}
            />
          </Field>
        </div>

        <Field label={t('packaging.lotCode')} hint={editing ? undefined : t('packaging.lotCodeHint')}>
          <div className="flex gap-2">
            <Input
              className="flex-1"
              value={form.lotCode}
              onChange={update('lotCode')}
              disabled={Boolean(editing)}
            />
            {!editing && (
              <Button variant="secondary" icon={Sparkles} onClick={suggest} title={t('packaging.suggestTitle')}>
                {t('packaging.suggest')}
              </Button>
            )}
          </div>
        </Field>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('packaging.packagingDate')}>
            <Input type="date" value={form.packagingDate} onChange={update('packagingDate')} />
          </Field>
          <label className="mt-6 flex items-center gap-2 text-sm text-text">
            <input
              type="checkbox"
              checked={form.exportReady}
              onChange={update('exportReady')}
              className="h-4 w-4 accent-primary"
            />
            {t('packaging.exportReady')}
          </label>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <OwnedResourceSelect
            label={t('packaging.equipment')}
            value={form.equipmentId}
            onChange={(id) => setForm((current) => ({ ...current, equipmentId: id }))}
            options={equipment}
            getOptionValue={(item) => item.equipmentId}
            getOptionLabel={(item) => t('packaging.equipmentOption', { name: item.equipmentName, id: item.equipmentId })}
            placeholder={t('packaging.noEquipment')}
            owner={OWNER_LINKS.equipment}
            showOwnerLinkAlways
          />
          <Field label={t('packaging.operator')}>
            <Select value={form.operatorId} onChange={update('operatorId')}>
              <option value="">{t('packaging.noOperator')}</option>
              {operators.map((operator) => (
                <option key={operator.operatorId} value={operator.operatorId}>
                  {operator.operatorName}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
