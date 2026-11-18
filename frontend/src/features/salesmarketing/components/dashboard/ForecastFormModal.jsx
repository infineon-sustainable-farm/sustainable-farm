import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select } from '../../../../shared/ui';

const SCENARIOS = ['base', 'optimistic', 'pessimistic'];
const RISKS = ['low', 'medium', 'high'];

/** New forecast dialog (POST /api/demand-forecasts). */
function ForecastFormModal({ open, form, setForm, products, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const set = (key) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [key]: value }));
  };

  return (
    <Modal open={open} onClose={onClose} size="lg" title={t('dashboard.form.title')}>
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <Field
          label={t('dashboard.form.product')}
          required
          hint={products.length === 0 ? t('dashboard.form.noProducts') : undefined}
        >
          <Select value={form.productId} onChange={set('productId')}>
            <option value="">{t('dashboard.form.selectProduct')}</option>
            {products.map((p) => (
              <option key={p.productId} value={p.productId}>
                {p.name} {p.variety ? `(${p.variety})` : ''}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label={t('dashboard.form.month')} required>
            <Input type="date" value={form.forecastMonth} onChange={set('forecastMonth')} />
          </Field>
          <Field label={t('dashboard.form.scenario')} required>
            <Select value={form.scenario} onChange={set('scenario')}>
              {SCENARIOS.map((value) => (
                <option key={value} value={value}>
                  {t(`dashboard.scenario.${value}`)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <Field label={t('dashboard.form.volume')} required>
            <Input type="number" min="0" step="0.01" value={form.forecastedVolumeT} onChange={set('forecastedVolumeT')} />
          </Field>
          <Field label={t('dashboard.form.lower')}>
            <Input type="number" min="0" step="0.01" value={form.lowerBoundT} onChange={set('lowerBoundT')} />
          </Field>
          <Field label={t('dashboard.form.upper')}>
            <Input type="number" min="0" step="0.01" value={form.upperBoundT} onChange={set('upperBoundT')} />
          </Field>
        </div>
        <Field label={t('dashboard.form.risk')}>
          <Select value={form.capacityRisk} onChange={set('capacityRisk')}>
            {RISKS.map((value) => (
              <option key={value} value={value}>
                {t(`dashboard.risk.${value}`)}
              </option>
            ))}
          </Select>
        </Field>
        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" icon={saving ? undefined : Plus} loading={saving}>
            {saving ? t('dashboard.form.saving') : t('dashboard.form.create')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default ForecastFormModal;
