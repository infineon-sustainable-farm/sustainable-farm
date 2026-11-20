import { useTranslation } from 'react-i18next'
import { Button, Field, Input, Modal, OwnedResourceSelect, Select } from '../../../../../shared/ui'
import { OWNER_LINKS } from '../../../../../app/ownerLinks'
import { FormActions } from '../../common/Screen'
import { FormAlert } from '../../common/FormAlert'
import { LocateHint } from './LocateHint'

const IRRIGATION_METHODS = ['drip', 'sprinkler', 'surface']

/**
 * Creation or edit of a field (parent farm), or creation of a zone (parent field), in a kit dialog.
 * The crop type of a field is a variety of the Plants catalogue (`varietyCatalog`: varieties,
 * loading, error, refetch), stored by its key.
 */
export function ChildFormModal({
  title, farms, fields, varietyCatalog, form, setForm, onCancel, onSave, busy, geo, onLocate, formError,
}) {
  const { t } = useTranslation('watersupply')
  const isZone = 'fieldId' in form
  const parentKey = isZone ? 'fieldId' : 'farmId'
  const parents = isZone ? fields : farms
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <Modal open onClose={onCancel} title={title} dismissible={!busy}>
      <FormAlert>{formError}</FormAlert>
      <form onSubmit={onSave} className="flex flex-col gap-4">
        <Field label={isZone ? t('farms.fields.parentField') : t('farms.fields.parentFarm')} required>
          <Select value={form[parentKey]} onChange={field(parentKey)}>
            <option value="">{t('farms.selectOption')}</option>
            {parents.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </Select>
        </Field>
        <Field label={t('farms.fields.name')} required>
          <Input value={form.name} onChange={field('name')} />
        </Field>
        {isZone && (
          <Field label={t('farms.fields.irrigationMethod')}>
            <Select value={form.irrigationMethod} onChange={field('irrigationMethod')}>
              <option value="">{t('farms.selectOption')}</option>
              {IRRIGATION_METHODS.map((method) => (
                <option key={method} value={method}>{t(`farms.methods.${method}`)}</option>
              ))}
            </Select>
          </Field>
        )}
        {!isZone && (
          <OwnedResourceSelect
            label={t('farms.fields.cropType')}
            value={form.cropType}
            onChange={(cropType) => setForm((p) => ({ ...p, cropType }))}
            options={varietyCatalog?.varieties ?? []}
            getOptionValue={(variety) => variety.varietyKey}
            getOptionLabel={(variety) => variety.varietyName}
            placeholder={t('farms.selectOption')}
            loading={varietyCatalog?.loading}
            loadError={Boolean(varietyCatalog?.error)}
            onRetry={varietyCatalog?.refetch}
            owner={OWNER_LINKS.variety}
          />
        )}
        {!isZone && (
          <Field label={t('farms.fields.code')} hint={t('farms.codeHint')}>
            <Input value={form.code} onChange={field('code')} placeholder={t('farms.codePlaceholder')} />
          </Field>
        )}
        <Field label={t('farms.fields.area')} required>
          <Input type="number" step="0.01" value={form.areaHectares} onChange={field('areaHectares')} />
        </Field>
        {!isZone && (
          <>
            <Field label={t('farms.fields.location')}>
              <Input value={form.coordinates} onChange={field('coordinates')} placeholder={t('farms.locationPlaceholder')} />
            </Field>
            <LocateHint geo={geo} onLocate={onLocate} />
          </>
        )}
        <FormActions>
          <Button variant="secondary" onClick={onCancel}>{t('common.cancel')}</Button>
          <Button type="submit" loading={busy}>{t('common.save')}</Button>
        </FormActions>
      </form>
    </Modal>
  )
}
