import { Plus, Save } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Field, Input, Modal, Select, Textarea } from '../../../../shared/ui';
import PersonSelect from '../../../../app/PersonSelect';
import { REPORT_TYPE_KEYS, REPORT_TYPES, SCHEDULE_FREQUENCIES, STATUS_OPTIONS } from './reportUtils';

/** Create or edit a demand report (POST /api/sales-marketing/demand-reports, PUT /api/sales-marketing/demand-reports/{id}). */
function ReportFormModal({ open, editingId, form, setForm, saving, onClose, onSubmit }) {
  const { t } = useTranslation('salesmarketing');
  const set = (key) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [key]: value }));
  };
  const editing = editingId != null;

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="xl"
      title={editing ? t('reports.editTitle', { id: editingId }) : t('reports.newCustom')}
      description={t('reports.formDescription')}
    >
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Field label={t('reports.fields.type')}>
            <Select value={form.reportType} onChange={set('reportType')}>
              {REPORT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`reports.types.${REPORT_TYPE_KEYS[type]}`)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('reports.fields.status')}>
            <Select value={form.status} onChange={set('status')}>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {t(`reports.statusOption.${status}`)}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Field label={t('reports.fields.periodStart')} required>
            <Input type="date" value={form.periodStart} onChange={set('periodStart')} />
          </Field>
          <Field label={t('reports.fields.periodEnd')} required>
            <Input type="date" value={form.periodEnd} onChange={set('periodEnd')} />
          </Field>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Field label={t('reports.fields.forecast')}>
            <Input
              type="number"
              min="0"
              step="0.01"
              value={form.totalForecastedVolumeT}
              onChange={set('totalForecastedVolumeT')}
              placeholder={t('reports.placeholders.forecast')}
            />
          </Field>
          <Field label={t('reports.fields.actual')}>
            <Input
              type="number"
              min="0"
              step="0.01"
              value={form.totalActualVolumeT}
              onChange={set('totalActualVolumeT')}
              placeholder={t('reports.placeholders.actual')}
            />
          </Field>
          <Field label={t('reports.fields.variance')}>
            <Input
              type="number"
              step="0.01"
              value={form.variancePct}
              onChange={set('variancePct')}
              placeholder={t('reports.placeholders.variance')}
            />
          </Field>
        </div>

        <div className="rounded-control bg-surface-muted p-4">
          <label className="flex cursor-pointer items-center gap-2 text-sm font-medium select-none">
            <input
              type="checkbox"
              checked={form.isScheduled}
              onChange={(event) => {
                const { checked } = event.target;
                setForm((current) => ({ ...current, isScheduled: checked }));
              }}
              className="h-5 w-5 accent-primary"
            />
            {t('reports.fields.scheduled')}
          </label>
          {form.isScheduled && (
            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              <Field label={t('reports.fields.nextRun')}>
                <Input type="datetime-local" value={form.nextRunAt} onChange={set('nextRunAt')} />
              </Field>
              <Field label={t('reports.fields.frequency')}>
                <Select value={form.scheduleFrequency} onChange={set('scheduleFrequency')}>
                  <option value="">{t('reports.selectPlaceholder')}</option>
                  {SCHEDULE_FREQUENCIES.map((frequency) => (
                    <option key={frequency} value={frequency}>
                      {t(`reports.frequencies.${frequency.toLowerCase()}`)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>
          )}
        </div>

        <PersonSelect
          label={t('reports.fields.generatedBy')}
          value={form.generatedByPersonId}
          currentName={form.generatedByName}
          onChange={(id) => setForm((current) => ({ ...current, generatedByPersonId: id }))}
          placeholder={t('reports.placeholders.generatedBy')}
        />
        <Field label={t('reports.fields.summary')}>
          <Textarea
            rows={3}
            value={form.summary}
            onChange={set('summary')}
            placeholder={t('reports.placeholders.summary')}
            className="resize-none"
          />
        </Field>

        <div className="mt-2 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            {t('common:actions.cancel')}
          </Button>
          <Button type="submit" loading={saving} icon={editing ? Save : Plus}>
            {saving ? t('reports.saving') : editing ? t('reports.saveChanges') : t('reports.createReport')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default ReportFormModal;
