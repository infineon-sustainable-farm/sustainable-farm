import { Plus, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select } from '../../../../shared/ui';
import { CUSTOMER_TYPES, PAYMENT_TERMS, STATUS_OPTIONS, useCustomerLabels } from './customerUtils';

/** Create or edit a customer. */
function CustomerFormModal({ open, editingId, form, setForm, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const labels = useCustomerLabels();
  const editing = editingId != null;
  const set = (key) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [key]: value }));
  };

  let submitLabel = t('customers.form.create');
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
      size="xl"
      title={editing ? t('customers.form.editTitle', { id: editingId }) : t('customers.form.newTitle')}
      description={t('customers.form.description')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('customers.form.companyName')} required>
          <Input value={form.companyName} onChange={set('companyName')} placeholder={t('customers.form.companyNamePlaceholder')} />
        </Field>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Field label={t('customers.form.customerCode')}>
            <Input value={form.customerCode} onChange={set('customerCode')} placeholder={t('customers.form.customerCodePlaceholder')} />
          </Field>
          <Field label={t('customers.form.customerType')}>
            <Select value={form.customerType} onChange={set('customerType')}>
              {CUSTOMER_TYPES.map((type) => (
                <option key={type} value={type}>
                  {labels.type(type)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Field label={t('customers.form.country')}>
            <Input value={form.country} onChange={set('country')} placeholder={t('customers.form.countryPlaceholder')} />
          </Field>
          <Field label={t('customers.form.region')}>
            <Input value={form.region} onChange={set('region')} placeholder={t('customers.form.regionPlaceholder')} />
          </Field>
          <Field label={t('customers.form.city')}>
            <Input value={form.city} onChange={set('city')} placeholder={t('customers.form.cityPlaceholder')} />
          </Field>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Field label={t('customers.form.status')}>
            <Select value={form.status} onChange={set('status')}>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {labels.status(status)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('customers.form.paymentTerms')}>
            <Select value={form.paymentTerms} onChange={set('paymentTerms')}>
              <option value="">{t('customers.form.select')}</option>
              {PAYMENT_TERMS.map((term) => (
                <option key={term} value={term}>
                  {labels.paymentTerms(term)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
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

export default CustomerFormModal;
