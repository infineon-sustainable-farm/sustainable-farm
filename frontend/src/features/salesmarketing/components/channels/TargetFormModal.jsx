import { Plus, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select } from '../../../../shared/ui';

/** Create or edit a revenue target of a channel for a period. */
function TargetFormModal({ open, editingId, form, setForm, channels, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const editing = editingId != null;
  const set = (key, parse = (value) => value) => (event) => {
    const value = parse(event.target.value);
    setForm((f) => ({ ...f, [key]: value }));
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={editing ? t('channels.editTargetTitle', { id: editingId }) : t('channels.newTargetTitle')}
      description={t('channels.targetFormHint')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('channels.fields.channel')} required>
          <Select value={form.channelId || ''} onChange={set('channelId', Number)}>
            <option value="" disabled>
              {t('channels.fields.selectChannel')}
            </option>
            {channels.map((ch) => (
              <option key={ch.channelId || ch.key} value={ch.channelId}>
                {ch.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Field label={t('channels.fields.periodStart')} required>
            <Input type="date" value={form.periodStart} onChange={set('periodStart')} />
          </Field>
          <Field label={t('channels.fields.periodEnd')} required>
            <Input type="date" value={form.periodEnd} onChange={set('periodEnd')} />
          </Field>
        </div>
        <Field label={t('channels.fields.revenueTarget')} required>
          <Input
            type="number"
            min="0"
            step="0.01"
            value={form.revenueTargetEur}
            onChange={set('revenueTargetEur')}
            placeholder={t('channels.fields.revenuePlaceholder')}
          />
        </Field>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving} icon={saving ? undefined : editing ? Save : Plus}>
            {saving ? t('channels.saving') : editing ? t('channels.saveChanges') : t('channels.createTarget')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default TargetFormModal;
