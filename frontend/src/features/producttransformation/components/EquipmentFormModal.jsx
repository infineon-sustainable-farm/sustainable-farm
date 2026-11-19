import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, Select } from '../../../shared/ui';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import { EQUIPMENT_TYPES, equipmentTypeLabel, maintenanceStatusLabel } from './equipmentOptions';

const toForm = (item) => ({
  equipmentType: item.equipmentType || 'WASHING',
  capacityKgPerHour: String(item.capacityKgPerHour ?? ''),
  energyConsumptionKwhPerKg: String(item.energyConsumptionKwhPerKg ?? ''),
  location: item.location || '',
  lastMaintenanceDate: item.lastMaintenanceDate ? String(item.lastMaintenanceDate).slice(0, 10) : '',
});

/**
 * Edit the PT specs of a machine: type, capacity, energy, location and last maintenance date.
 * The machine itself (code, name, status) belongs to Machinery and is shown read-only. `onSubmit(form)` returns a promise; it rejects with the server
 * error.
 */
export default function EquipmentFormModal({ open, editing, onClose, onSubmit }) {
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
    if (!form.equipmentType) return t('equipment.validation.typeRequired');
    if (form.capacityKgPerHour === '' || Number(form.capacityKgPerHour) <= 0) {
      return t('equipment.validation.capacityPositive');
    }
    if (form.energyConsumptionKwhPerKg === '' || Number(form.energyConsumptionKwhPerKg) <= 0) {
      return t('equipment.validation.energyPositive');
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
      setFormError(err?.message || t('equipment.saveError'));
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={close} dismissible={!saving} size="lg" title={t('equipment.editTitle')}>
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Field label={t('equipment.equipmentId')}>
            <Input value={editing.equipmentId || ''} disabled />
          </Field>
          <Field label={t('equipment.name')}>
            <Input value={editing.equipmentName || ''} disabled />
          </Field>
          <Field label={t('equipment.maintenanceStatus')}>
            <Input value={maintenanceStatusLabel(editing.maintenanceStatus)} disabled />
          </Field>
        </div>
        <p className="text-xs text-text-muted">{t('equipment.machineryNote')}</p>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('equipment.type')}>
            <Select value={form.equipmentType} onChange={update('equipmentType')}>
              {EQUIPMENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {equipmentTypeLabel(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('equipment.location')}>
            <Input value={form.location} maxLength={100} onChange={update('location')} />
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('equipment.capacity')}>
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.capacityKgPerHour}
              onChange={update('capacityKgPerHour')}
            />
          </Field>
          <Field label={t('equipment.energy')}>
            <Input
              type="number"
              min="0.01"
              step="0.01"
              value={form.energyConsumptionKwhPerKg}
              onChange={update('energyConsumptionKwhPerKg')}
            />
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('equipment.lastMaintenance')}>
            <Input type="date" value={form.lastMaintenanceDate} onChange={update('lastMaintenanceDate')} />
          </Field>
        </div>

        <FormActions onCancel={close} saving={saving} editing />
      </form>
    </Modal>
  );
}
