import { useTranslation } from 'react-i18next'
import { Button, Field, Input, Modal, Textarea } from '../../../../../shared/ui'
import { FormActions } from '../../common/Screen'
import { FormAlert } from '../../common/FormAlert'
import { LocateHint } from './LocateHint'

/** Creation or edition of a farm, in a kit dialog. */
export function FarmFormModal({ editing, form, setForm, onCancel, onSave, busy, geo, onLocate, formError }) {
  const { t } = useTranslation('watersupply')
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <Modal open onClose={onCancel} title={editing ? t('farms.editFarm') : t('farms.newFarm')} dismissible={!busy}>
      <FormAlert>{formError}</FormAlert>
      <form onSubmit={onSave} className="flex flex-col gap-4">
        <Field label={t('farms.fields.name')} required>
          <Input value={form.name} onChange={field('name')} />
        </Field>
        <Field label={t('farms.fields.address')}>
          <Input value={form.address} onChange={field('address')} />
        </Field>
        <Field label={t('farms.fields.area')}>
          <Input type="number" step="0.01" value={form.areaHectares} onChange={field('areaHectares')} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label={t('farms.fields.latitude')}>
            <Input type="number" step="any" min="-90" max="90" value={form.latitude} onChange={field('latitude')} />
          </Field>
          <Field label={t('farms.fields.longitude')}>
            <Input type="number" step="any" min="-180" max="180" value={form.longitude} onChange={field('longitude')} />
          </Field>
        </div>
        <LocateHint geo={geo} onLocate={onLocate} />
        <Field label={t('farms.fields.description')}>
          <Textarea value={form.description} onChange={field('description')} />
        </Field>
        <FormActions>
          <Button variant="secondary" onClick={onCancel}>{t('common.cancel')}</Button>
          <Button type="submit" loading={busy}>{t('common.save')}</Button>
        </FormActions>
      </form>
    </Modal>
  )
}
