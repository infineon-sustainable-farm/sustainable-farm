import { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, Card, ConfirmDialog, EmptyState } from '../../../../shared/ui'
import { farmApi, fieldApi, zoneApi } from '../../api/watersupplyApi'
import { coordinatesError, useGeolocation } from '../../hooks/useGeolocation'
import { Filters, Screen } from '../common/Screen'
import { FormAlert } from '../common/FormAlert'
import { LoadingState } from '../common/LoadingState'
import { SearchField } from '../common/SearchField'
import { ChildFormModal } from './farms/ChildFormModal'
import { FarmFormModal } from './farms/FarmFormModal'
import { FarmsTable, FieldsTable, ZonesTable } from './farms/FarmTables'
import { formatFieldCoordinates, parseFieldCoordinates, useGeoMessage } from './farms/coordinates'

const EMPTY_FARM_FORM = { name: '', address: '', areaHectares: '', description: '', latitude: '', longitude: '' }

/**
 * CRUD view for Farms / Fields / Zones (Task 4.1). The tables, the forms and the coordinate
 * helpers live in ./farms.
 *
 * Location: when a farm or a field is created, its position is taken from the device's
 * geolocation (the person is expected to be on site) and can be corrected by hand. TO BE
 * REVIEWED: first version, see useGeolocation.
 */
export function FarmsView({ notify }) {
  const { t } = useTranslation('watersupply')
  const geoMessage = useGeoMessage()
  const [farms, setFarms] = useState([])
  const [fields, setFields] = useState([])
  const [zones, setZones] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedFarm, setSelectedFarm] = useState(null)

  const [showFarmForm, setShowFarmForm] = useState(false)
  const [editingFarm, setEditingFarm] = useState(null)
  const [farmForm, setFarmForm] = useState(EMPTY_FARM_FORM)
  const [formError, setFormError] = useState(null)
  const geo = useGeolocation()
  const [deleteTarget, setDeleteTarget] = useState(null) // {kind, id, title}
  const [busy, setBusy] = useState(false)

  const [showFieldForm, setShowFieldForm] = useState(false)
  const [fieldForm, setFieldForm] = useState({ farmId: '', name: '', areaHectares: '', cropType: '', soilType: '', coordinates: '' })
  const [showZoneForm, setShowZoneForm] = useState(false)
  const [zoneForm, setZoneForm] = useState({ fieldId: '', name: '', areaHectares: '', irrigationMethod: '' })
  const [query, setQuery] = useState('')

  const loadAll = useCallback(() => {
    setLoading(true)
    setError(null)
    Promise.all([farmApi.getFarms(), fieldApi.getFields(), zoneApi.getZones()])
      .then(([f, fi, z]) => {
        setFarms(f)
        setFields(fi)
        setZones(z)
      })
      .catch((e) => setError(e.message || t('farms.loadError')))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    const timeoutId = window.setTimeout(loadAll, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadAll])
  const filteredFields = selectedFarm ? fields.filter((f) => f.farmId === selectedFarm) : fields

  // Global search: filters farms, fields and zones by name.
  const matchesQuery = useCallback(
    (item) => {
      const q = query.trim().toLowerCase()
      return !q || String(item?.name || '').toLowerCase().includes(q)
    },
    [query],
  )
  const visibleFarms = useMemo(() => farms.filter(matchesQuery), [farms, matchesQuery])
  const visibleFields = useMemo(() => filteredFields.filter(matchesQuery), [filteredFields, matchesQuery])
  const visibleZones = useMemo(() => zones.filter(matchesQuery), [zones, matchesQuery])

  /** Fills the position from the device, unless the user already typed one meanwhile. */
  const locateFarm = () => geo.locate().then((position) => {
    if (!position) return
    setFarmForm((current) => (current.latitude === '' && current.longitude === ''
      ? { ...current, latitude: String(position.latitude), longitude: String(position.longitude) }
      : current))
  })

  const locateField = () => geo.locate().then((position) => {
    if (!position) return
    setFieldForm((current) => (current.coordinates === ''
      ? { ...current, coordinates: formatFieldCoordinates(position.latitude, position.longitude) }
      : current))
  })

  const openFarmForm = (farm) => {
    setFormError(null)
    if (farm) {
      setEditingFarm(farm)
      setFarmForm({
        name: farm.name || '',
        address: farm.address || '',
        areaHectares: farm.areaHectares ?? '',
        description: farm.description || '',
        latitude: farm.latitude ?? '',
        longitude: farm.longitude ?? '',
      })
      setShowFarmForm(true)
    } else {
      setEditingFarm(null)
      setFarmForm(EMPTY_FARM_FORM)
      setShowFarmForm(true)
      // A new farm is created on site: its position is requested right away.
      locateFarm()
    }
  }

  const openFieldForm = () => {
    setFormError(null)
    setFieldForm((current) => ({ ...current, coordinates: '' }))
    setShowFieldForm(true)
    locateField()
  }

  const saveFarm = (e) => {
    e.preventDefault()
    const invalid = coordinatesError(String(farmForm.latitude), String(farmForm.longitude))
    if (invalid) {
      setFormError(geoMessage(invalid))
      return
    }
    setFormError(null)
    setBusy(true)
    // The API replaces the position on every update: it is always sent, otherwise an edit erases it.
    const payload = {
      name: farmForm.name,
      address: farmForm.address || null,
      areaHectares: farmForm.areaHectares === '' ? null : Number(farmForm.areaHectares),
      description: farmForm.description || null,
      latitude: farmForm.latitude === '' ? null : Number(farmForm.latitude),
      longitude: farmForm.longitude === '' ? null : Number(farmForm.longitude),
    }
    const req = editingFarm ? farmApi.updateFarm(editingFarm.id, payload) : farmApi.createFarm(payload)
    req
      .then(() => {
        notify(editingFarm ? t('farms.farmUpdated') : t('farms.farmCreated'))
        setShowFarmForm(false)
        loadAll()
      })
      .catch((err) => setError(err.message || t('farms.saveError')))
      .finally(() => setBusy(false))
  }

  const saveField = (e) => {
    e.preventDefault()
    const coordinates = fieldForm.coordinates.trim()
    const parsed = parseFieldCoordinates(coordinates)
    const invalid = coordinates && (parsed ? coordinatesError(parsed.latitude, parsed.longitude) : t('farms.errors.locationFormat'))
    if (invalid) {
      setFormError(geoMessage(invalid))
      return
    }
    setFormError(null)
    setBusy(true)
    const payload = {
      farmId: fieldForm.farmId,
      name: fieldForm.name,
      areaHectares: fieldForm.areaHectares === '' ? 0 : Number(fieldForm.areaHectares),
      cropType: fieldForm.cropType || null,
      soilType: fieldForm.soilType || null,
      coordinates: parsed ? formatFieldCoordinates(Number(parsed.latitude), Number(parsed.longitude)) : null,
    }
    fieldApi
      .createField(payload)
      .then(() => {
        notify(t('farms.fieldCreated'))
        setShowFieldForm(false)
        setFieldForm((p) => ({ ...p, name: '', areaHectares: '', cropType: '', soilType: '', coordinates: '' }))
        loadAll()
      })
      .catch((err) => setError(err.message || t('farms.saveError')))
      .finally(() => setBusy(false))
  }

  const saveZone = (e) => {
    e.preventDefault()
    setBusy(true)
    const payload = {
      fieldId: zoneForm.fieldId,
      name: zoneForm.name,
      areaHectares: zoneForm.areaHectares === '' ? 0 : Number(zoneForm.areaHectares),
      irrigationMethod: zoneForm.irrigationMethod || null,
    }
    zoneApi
      .createZone(payload)
      .then(() => {
        notify(t('farms.zoneCreated'))
        setShowZoneForm(false)
        setZoneForm((p) => ({ ...p, name: '', areaHectares: '', irrigationMethod: '' }))
        loadAll()
      })
      .catch((err) => setError(err.message || t('farms.saveError')))
      .finally(() => setBusy(false))
  }

  const confirmDelete = () => {
    if (!deleteTarget) return
    setBusy(true)
    const api = { farm: farmApi, field: fieldApi, zone: zoneApi }[deleteTarget.kind]
    const fn = deleteTarget.kind === 'farm'
      ? () => api.deleteFarm(deleteTarget.id)
      : deleteTarget.kind === 'field'
        ? () => api.deleteField(deleteTarget.id)
        : () => api.deleteZone(deleteTarget.id)
    fn()
      .then(() => {
        notify(t('farms.deleted'))
        setDeleteTarget(null)
        loadAll()
      })
      .catch((err) => setError(err.message || t('farms.deleteError')))
      .finally(() => setBusy(false))
  }
  const newButton = (label, onClick) => (
    <Button size="sm" icon={Plus} onClick={onClick}>{label}</Button>
  )

  return (
    <Screen title={t('farms.title')} subtitle={t('farms.subtitle')}>
      <FormAlert>{error}</FormAlert>

      <Filters>
        <SearchField value={query} onChange={setQuery} placeholder={t('farms.search')} />
      </Filters>

      {loading ? (
        <LoadingState label={t('farms.loading')} />
      ) : (
        <>
          <Card title={t('farms.farmsTitle')} actions={newButton(t('farms.newFarm'), () => openFarmForm(null))}>
            {visibleFarms.length === 0 ? (
              <EmptyState title={t('farms.noFarmsTitle')} message={t('farms.noFarms')} />
            ) : (
              <FarmsTable
                rows={visibleFarms}
                selectedFarm={selectedFarm}
                onEdit={openFarmForm}
                onDelete={(row) => setDeleteTarget({ kind: 'farm', id: row.id, title: row.name })}
                onToggleFields={(id) => setSelectedFarm((prev) => (prev === id ? null : id))}
              />
            )}
          </Card>

          <Card
            title={selectedFarm ? t('farms.fieldsFilteredTitle') : t('farms.fieldsTitle')}
            actions={newButton(t('farms.newField'), openFieldForm)}
          >
            {visibleFields.length === 0 ? (
              <EmptyState title={t('farms.noFieldsTitle')} message={t('farms.noFields')} />
            ) : (
              <FieldsTable rows={visibleFields} onDelete={(row) => setDeleteTarget({ kind: 'field', id: row.id, title: row.name })} />
            )}
          </Card>

          <Card title={t('farms.zonesTitle')} actions={newButton(t('farms.newZone'), () => setShowZoneForm(true))}>
            {visibleZones.length === 0 ? (
              <EmptyState title={t('farms.noZonesTitle')} message={t('farms.noZones')} />
            ) : (
              <ZonesTable rows={visibleZones} onDelete={(row) => setDeleteTarget({ kind: 'zone', id: row.id, title: row.name })} />
            )}
          </Card>
        </>
      )}

      {showFarmForm && (
        <FarmFormModal
          editing={editingFarm}
          form={farmForm}
          setForm={setFarmForm}
          onCancel={() => setShowFarmForm(false)}
          onSave={saveFarm}
          busy={busy}
          geo={geo}
          onLocate={() => {
            setFarmForm((current) => ({ ...current, latitude: '', longitude: '' }))
            locateFarm()
          }}
          formError={formError}
        />
      )}

      {showFieldForm && (
        <ChildFormModal
          title={t('farms.newField')}
          farms={farms}
          fields={[]}
          form={fieldForm}
          setForm={setFieldForm}
          onCancel={() => setShowFieldForm(false)}
          onSave={saveField}
          busy={busy}
          geo={geo}
          onLocate={() => {
            setFieldForm((current) => ({ ...current, coordinates: '' }))
            locateField()
          }}
          formError={formError}
        />
      )}

      {showZoneForm && (
        <ChildFormModal
          title={t('farms.newZone')}
          farms={farms}
          fields={fields}
          form={zoneForm}
          setForm={setZoneForm}
          onCancel={() => setShowZoneForm(false)}
          onSave={saveZone}
          busy={busy}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('farms.deleteTitle', { name: deleteTarget?.title })}
        message={t('farms.deleteMessage')}
        onConfirm={confirmDelete}
        onClose={() => setDeleteTarget(null)}
        loading={busy}
      />
    </Screen>
  )
}
