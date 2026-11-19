import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select } from '../../../../shared/ui';
import { RISKS, SCENARIOS } from './forecastingData';

/** Create or edit a demand forecast row. */
function ForecastFormModal({ open, editing, form, setForm, products, saving, onClose, onSubmit }) {
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
      title={editing ? t('forecasting.form.editTitle', { id: editing.forecastId }) : t('forecasting.form.addTitle')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field label={t('forecasting.form.product')} required>
          <Select value={form.productId} onChange={set('productId')}>
            <option value="">{t('forecasting.form.select')}</option>
            {products.map((p) => (
              <option key={p.productId} value={p.productId}>
                {p.variety ? t('forecasting.form.productOption', { name: p.name, variety: p.variety }) : p.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('forecasting.form.month')} required>
            <Input type="date" value={form.forecastMonth} onChange={set('forecastMonth')} />
          </Field>
          <Field label={t('forecasting.form.scenario')} required>
            <Select value={form.scenario} onChange={set('scenario')}>
              {SCENARIOS.map((scenario) => (
                <option key={scenario} value={scenario}>
                  {t(`forecasting.scenario.${scenario}`)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <Field label={t('forecasting.form.volume')} required>
            <Input type="number" min="0" step="0.01" value={form.forecastedVolumeT} onChange={set('forecastedVolumeT')} />
          </Field>
          <Field label={t('forecasting.form.lower')}>
            <Input type="number" min="0" step="0.01" value={form.lowerBoundT} onChange={set('lowerBoundT')} />
          </Field>
          <Field label={t('forecasting.form.upper')}>
            <Input type="number" min="0" step="0.01" value={form.upperBoundT} onChange={set('upperBoundT')} />
          </Field>
        </div>
        <Field label={t('forecasting.form.risk')}>
          <Select value={form.capacityRisk} onChange={set('capacityRisk')}>
            {RISKS.map((risk) => (
              <option key={risk} value={risk}>
                {t(`forecasting.riskOption.${risk}`)}
              </option>
            ))}
          </Select>
        </Field>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving}>
            {saving ? t('forecasting.form.saving') : editing ? t('forecasting.form.update') : t('forecasting.form.create')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default ForecastFormModal;
