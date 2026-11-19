import { Plus, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal } from '../../../../shared/ui';

/** Create or edit a sales channel. */
function ChannelFormModal({ open, editingId, form, setForm, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const editing = editingId != null;
  return (
    <Modal
      open={open}
      onClose={onClose}
      size="xl"
      title={editing ? t('channels.editChannelTitle', { id: editingId }) : t('channels.newChannelTitle')}
      description={t('channels.channelFormHint')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('channels.fields.name')} required>
          <Input
            type="text"
            value={form.name}
            onChange={(e) => {
              const { value } = e.target;
              setForm((f) => ({ ...f, name: value }));
            }}
            placeholder={t('channels.fields.namePlaceholder')}
          />
        </Field>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving} icon={saving ? undefined : editing ? Save : Plus}>
            {saving ? t('channels.saving') : editing ? t('channels.saveChanges') : t('channels.createChannel')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default ChannelFormModal;
