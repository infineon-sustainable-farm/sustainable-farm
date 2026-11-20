import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select, Textarea } from '../../../../shared/ui';
import { SEVERITIES, STATUSES } from './alertUtils';

/** Create or edit an alert. */
function AlertFormModal({ open, editing, form, setForm, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const set = (key) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [key]: value }));
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={editing ? t('alerts.editTitle', { id: editing.alertId }) : t('alerts.newTitle')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('alerts.fields.code')}>
            <Input value={form.alertCode} onChange={set('alertCode')} />
          </Field>
          <Field label={t('alerts.fields.type')} required>
            <Input value={form.alertType} onChange={set('alertType')} />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('alerts.fields.severity')} required>
            <Select value={form.severity} onChange={set('severity')}>
              {SEVERITIES.map((value) => (
                <option key={value} value={value}>
                  {t(`alerts.severity.${value}`)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('alerts.fields.status')}>
            <Select value={form.status} onChange={set('status')}>
              {STATUSES.map((value) => (
                <option key={value} value={value}>
                  {t(`alerts.status.${value}`)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label={t('alerts.fields.message')} required>
          <Textarea rows={3} value={form.message} onChange={set('message')} className="resize-y" />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('alerts.fields.region')}>
            <Input value={form.region} onChange={set('region')} />
          </Field>
          <Field label={t('alerts.fields.origin')}>
            <Input value={form.origin} onChange={set('origin')} />
          </Field>
        </div>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving}>
            {saving ? t('alerts.saving') : editing ? t('alerts.update') : t('alerts.create')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default AlertFormModal;
