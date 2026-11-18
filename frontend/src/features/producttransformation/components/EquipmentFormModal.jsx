import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Modal, Select } from '../../../shared/ui';
import FormActions from './FormActions';
import FormAlert from './FormAlert';
import {
  EQUIPMENT_NAME_MAX_LENGTH,
  EQUIPMENT_TYPES,
  MAINTENANCE_STATUSES,
  equipmentTypeLabel,
  maintenanceStatusLabel,
} from './equipmentOptions';

const toForm = (item) => ({
  equipmentId: item?.equipmentId || '',
  equipmentName: item?.equipmentName || '',
  equipmentType: item?.equipmentType || 'WASHING',
  capacityKgPerHour: item ? String(item.capacityKgPerHour ?? '') : '',
  energyConsumptionKwhPerKg: item ? String(item.energyConsumptionKwhPerKg ?? '') : '',
  location: item?.location || '',
  maintenanceStatus: item?.maintenanceStatus || 'ACTIVE',
  lastMaintenanceDate: item?.lastMaintenanceDate ? String(item.lastMaintenanceDate).slice(0, 10) : '',
});

/**
 * Create or edit the PT attributes of a machine. The equipment ID (the machine code of the farm
 * register) is set at creation and read-only afterwards. `onSubmit(form)` returns a promise; it
 * rejects with the server error.
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
    if (!editing && !form.equipmentId.trim()) return t('equipment.validation.idRequired');
    if (!form.equipmentName.trim()) return t('equipment.validation.nameRequired');
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
    <Modal
      open={open}
      onClose={close}
      dismissible={!saving}
      size="lg"
      title={editing ? t('equipment.editTitle') : t('equipment.newTitle')}
    >
      {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label={t('equipment.equipmentId')}>
            <Input value={form.equipmentId} onChange={update('equipmentId')} disabled={Boolean(editing)} />
          </Field>
          <Field label={t('equipment.name')}>
            <Input
              value={form.equipmentName}
              maxLength={EQUIPMENT_NAME_MAX_LENGTH}
              onChange={update('equipmentName')}
            />
          </Field>
        </div>

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
          <Field label={t('equipment.maintenanceStatus')}>
            <Select value={form.maintenanceStatus} onChange={update('maintenanceStatus')}>
              {MAINTENANCE_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {maintenanceStatusLabel(status)}
                </option>
              ))}
            </Select>
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
          <Field label={t('equipment.location')}>
            <Input value={form.location} onChange={update('location')} />
          </Field>
          <Field label={t('equipment.lastMaintenance')}>
            <Input type="date" value={form.lastMaintenanceDate} onChange={update('lastMaintenanceDate')} />
          </Field>
        </div>

        <FormActions onCancel={close} saving={saving} editing={Boolean(editing)} />
      </form>
    </Modal>
  );
}
