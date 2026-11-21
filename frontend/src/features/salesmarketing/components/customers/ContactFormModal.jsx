import { Plus, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal } from '../../../../shared/ui';

/** Create or edit a contact of the selected customer. */
function ContactFormModal({ open, editing, form, setForm, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const set = (key) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [key]: value }));
  };

  let submitLabel = t('customers.contactForm.add');
  let submitIcon = Plus;
  if (saving) submitLabel = t('customers.form.saving');
  else if (editing) {
    submitLabel = t('customers.form.saveChanges');
    submitIcon = Save;
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={editing ? t('customers.contactForm.editTitle') : t('customers.contactForm.newTitle')}
      description={t('customers.contactForm.description')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('customers.contactForm.fullName')} required>
          <Input value={form.fullName} onChange={set('fullName')} />
        </Field>
        <Field label={t('customers.contactForm.role')}>
          <Input value={form.role} onChange={set('role')} placeholder={t('customers.contactForm.rolePlaceholder')} />
        </Field>
        <Field label={t('customers.contactForm.email')}>
          <Input type="email" value={form.email} onChange={set('email')} placeholder={t('customers.contactForm.emailPlaceholder')} />
        </Field>
        <Field label={t('customers.contactForm.phone')}>
          <Input type="tel" value={form.phone} onChange={set('phone')} placeholder={t('customers.contactForm.phonePlaceholder')} />
        </Field>
        <label className="flex cursor-pointer items-center gap-2 text-sm font-medium select-none">
          <input
            type="checkbox"
            checked={form.isPrimary}
            onChange={(e) => {
              const { checked } = e.target;
              setForm((current) => ({ ...current, isPrimary: checked }));
            }}
            className="h-5 w-5 accent-primary"
          />
          {t('customers.contactForm.primary')}
        </label>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" icon={saving ? undefined : submitIcon} loading={saving}>
            {submitLabel}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default ContactFormModal;
