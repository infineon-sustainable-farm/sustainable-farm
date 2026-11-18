import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select } from '../../../../shared/ui';
import { CAMPAIGN_STATUSES } from './campaignFormat';

/** Create or edit a campaign. */
function CampaignFormModal({ open, editing, form, setForm, saving, onClose, onSubmit }) {
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
      title={editing ? t('campaigns.editTitle', { id: editing.campaignId }) : t('campaigns.newTitle')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('campaigns.fields.name')} required>
          <Input value={form.name} onChange={set('name')} />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('campaigns.fields.status')}>
            <Select value={form.status} onChange={set('status')}>
              {CAMPAIGN_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {t(`campaigns.status.${status}`)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('campaigns.fields.currency')}>
            <Select value={form.currency} onChange={set('currency')}>
              <option value="EUR">{t('campaigns.currencyEur')}</option>
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('campaigns.fields.budget')}>
            <Input type="number" min="0" step="0.01" value={form.budgetEur} onChange={set('budgetEur')} />
          </Field>
          <Field label={t('campaigns.fields.spent')}>
            <Input type="number" min="0" step="0.01" value={form.budgetSpentEur} onChange={set('budgetSpentEur')} />
          </Field>
        </div>
        <Field label={t('campaigns.fields.reach')}>
          <Input type="number" min="0" value={form.estimatedReach} onChange={set('estimatedReach')} />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('campaigns.fields.startDate')}>
            <Input type="date" value={form.startDate} onChange={set('startDate')} />
          </Field>
          <Field label={t('campaigns.fields.endDate')}>
            <Input type="date" value={form.endDate} onChange={set('endDate')} />
          </Field>
        </div>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving}>
            {saving ? t('campaigns.saving') : editing ? t('campaigns.update') : t('campaigns.create')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default CampaignFormModal;
