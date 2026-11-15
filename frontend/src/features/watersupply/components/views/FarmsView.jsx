import { useCallback, useEffect, useMemo, useState } from 'react'
import { LocateFixed, Plus, Pencil, Trash2 } from 'lucide-react'
import { farmApi, fieldApi, zoneApi } from '../../api/watersupplyApi'
import { coordinatesError, mapUrl, useGeolocation } from '../../hooks/useGeolocation'
import { DataTable } from '../../../../shared/components/DataTable'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'
import { EmptyState } from './EmptyState'
import { Spinner } from '../../../../shared/components/Spinner'

const sectionLabel = {
  fontFamily: "'Montserrat', Arial, sans-serif",
  fontWeight: 600,
  fontSize: '16px',
  color: 'var(--ws-ink, #1c2b29)',
  margin: '0 0 12px',
}

const panelBox = {
  background: 'var(--ws-card, #fff)',
  border: '1px solid var(--ws-line, #e2e9e7)',
  borderRadius: '12px',
  padding: '18px',
  marginBottom: '22px',
}

const btn = (kind) => ({
  display: 'inline-flex',
  alignItems: 'center',
  gap: '6px',
  padding: '8px 14px',
  border: 'none',
  borderRadius: '8px',
  fontSize: '13px',
  fontWeight: 600,
  fontFamily: "'Inter', Arial, sans-serif",
  cursor: 'pointer',
  color: kind === 'primary' ? '#fff' : 'var(--ws-ink, #1c2b29)',
  background: kind === 'primary' ? 'var(--ws-primary, #0a8276)' : 'var(--ws-line, #e2e9e7)',
})

const btnDanger = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '6px',
  padding: '7px 10px',
  border: 'none',
  borderRadius: '8px',
  fontSize: '13px',
  fontWeight: 600,
  fontFamily: "'Inter', Arial, sans-serif",
  cursor: 'pointer',
  color: 'var(--ws-red, #c62828)',
  background: 'var(--ws-red-tint, #fbeaea)',
}

/** Field coordinates are stored as text by the API: "lat, lng". */
function formatFieldCoordinates(latitude, longitude) {
  return `${latitude}, ${longitude}`
}

function parseFieldCoordinates(text) {
  const match = /^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$/.exec(text || '')
  return match ? { latitude: match[1], longitude: match[2] } : null
}

/** Position cell of the tables: coordinates with a link to check them on a map. */
function LocationCell({ latitude, longitude }) {
  if (latitude == null || longitude == null || latitude === '' || longitude === '') return '—'
  return (
    <a href={mapUrl(latitude, longitude)} target="_blank" rel="noopener noreferrer" title="Open on OpenStreetMap">
      {Number(latitude).toFixed(4)}, {Number(longitude).toFixed(4)}
    </a>
  )
}

const EMPTY_FARM_FORM = { name: '', address: '', areaHectares: '', description: '', latitude: '', longitude: '' }

/**
 * CRUD view for Farms / Fields / Zones (Task 4.1).
 * Uses the reusable design-system components.
 *
 * Location: when a farm or a field is created, its position is taken from the device's
 * geolocation (the person is expected to be on site) and can be corrected by hand. TO BE
 * REVIEWED: first version, see useGeolocation.
 */
export function FarmsView({ notify }) {
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
      .catch((e) => setError(e.message || 'Loading error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(loadAll, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadAll])
const farmColumns = [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'address', label: 'Address' },
    { key: 'areaHectares', label: 'Area (ha)', sortable: true, render: (r) => `${r.areaHectares ?? 0} ha` },
    { key: 'description', label: 'Description', render: (r) => r.description || '—' },
    { key: 'latitude', label: 'Location', render: (r) => <LocationCell latitude={r.latitude} longitude={r.longitude} /> },
  ]

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
      setFormError(invalid)
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
        notify(editingFarm ? 'Farm updated' : 'Farm created')
        setShowFarmForm(false)
        loadAll()
      })
      .catch((err) => setError(err.message || 'Save error'))
      .finally(() => setBusy(false))
  }

  const saveField = (e) => {
    e.preventDefault()
    const coordinates = fieldForm.coordinates.trim()
    const parsed = parseFieldCoordinates(coordinates)
    const invalid = coordinates && (parsed ? coordinatesError(parsed.latitude, parsed.longitude) : 'Write the location as "latitude, longitude".')
    if (invalid) {
      setFormError(invalid)
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
        notify('Field created')
        setShowFieldForm(false)
        setFieldForm((p) => ({ ...p, name: '', areaHectares: '', cropType: '', soilType: '', coordinates: '' }))
        loadAll()
      })
      .catch((err) => setError(err.message || 'Save error'))
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
        notify('Zone created')
        setShowZoneForm(false)
        setZoneForm((p) => ({ ...p, name: '', areaHectares: '', irrigationMethod: '' }))
        loadAll()
      })
      .catch((err) => setError(err.message || 'Save error'))
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
        notify('Deleted')
        setDeleteTarget(null)
        loadAll()
      })
      .catch((err) => setError(err.message || 'Delete error'))
      .finally(() => setBusy(false))
  }
return (
    <div>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Farms, fields & zones</h1>
          <p>Manage production sites, their fields and irrigation zones.</p>
        </div>
      </div>

      {error && (
        <div style={{ ...panelBox, borderColor: 'var(--ws-red, #c62828)', color: 'var(--ws-red, #c62828)' }}>
          {error}
        </div>
      )}

      <div className="ws-filters">
        <SearchInput value={query} onChange={setQuery} placeholder="Search a farm, a field, a zone…" />
      </div>

      {loading ? (
        <Spinner label="Loading data…" full />
      ) : (
        <>
          <div style={panelBox}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h2 style={sectionLabel}>Farms</h2>
              <button style={btn('primary')} onClick={() => openFarmForm(null)}>
                <Plus size={15} /> New farm
              </button>
            </div>
            {visibleFarms.length === 0 ? (
              <EmptyState title="No farms" description="Create your first farm." />
            ) : (
              <DataTable
                columns={farmColumns}
                rows={visibleFarms}
                keyField="id"
                actions={(row) => (
                  <span style={{ display: 'inline-flex', gap: '6px', alignItems: 'center' }}>
                    <button title="Edit" onClick={() => openFarmForm(row)} style={btn('ghost')}>
                      <Pencil size={14} />
                    </button>
                    <button title="Delete" onClick={() => setDeleteTarget({ kind: 'farm', id: row.id, title: row.name })} style={btnDanger}>
                      <Trash2 size={14} />
                    </button>
                    <button style={{ padding: '7px 10px', ...btn('ghost') }} onClick={() => setSelectedFarm((prev) => (prev === row.id ? null : row.id))}>
                      {selectedFarm === row.id ? 'All' : 'Fields'}
                    </button>
                  </span>
                )}
              />
            )}
          </div>

          <div style={panelBox}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h2 style={sectionLabel}>{selectedFarm ? 'Fields (filtered)' : 'Fields'}</h2>
              <button style={btn('primary')} onClick={openFieldForm}>
                <Plus size={15} /> New field
              </button>
            </div>
            {visibleFields.length === 0 ? (
              <EmptyState title="No fields" description="Add a field to a farm." />
            ) : (
              <DataTable
                columns={[
                  { key: 'name', label: 'Name', sortable: true },
                  { key: 'cropType', label: 'Crop', render: (r) => r.cropType || '—' },
                  { key: 'soilType', label: 'Soil', render: (r) => r.soilType || '—' },
                  { key: 'areaHectares', label: 'Area (ha)', render: (r) => `${r.areaHectares ?? 0} ha` },
                  {
                    key: 'coordinates',
                    label: 'Location',
                    render: (r) => {
                      const position = parseFieldCoordinates(r.coordinates)
                      return position ? <LocationCell {...position} /> : (r.coordinates || '—')
                    },
                  },
                ]}
                rows={visibleFields}
                keyField="id"
                actions={(row) => (
                  <button title="Delete" style={btnDanger} onClick={() => setDeleteTarget({ kind: 'field', id: row.id, title: row.name })}>
                    <Trash2 size={14} />
                  </button>
                )}
              />
            )}
          </div>
<div style={panelBox}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h2 style={sectionLabel}>Zones</h2>
              <button style={btn('primary')} onClick={() => setShowZoneForm(true)}>
                <Plus size={15} /> New zone
              </button>
            </div>
            {visibleZones.length === 0 ? (
              <EmptyState title="No zones" description="Add irrigation zones." />
            ) : (
              <DataTable
                columns={[
                  { key: 'name', label: 'Name', sortable: true },
                  { key: 'irrigationMethod', label: 'Irrigation method', render: (r) => r.irrigationMethod || '—' },
                  { key: 'areaHectares', label: 'Area (ha)', render: (r) => `${r.areaHectares ?? 0} ha` },
                ]}
                rows={visibleZones}
                keyField="id"
                actions={(row) => (
                  <button title="Delete" style={btnDanger} onClick={() => setDeleteTarget({ kind: 'zone', id: row.id, title: row.name })}>
                    <Trash2 size={14} />
                  </button>
                )}
              />
            )}
          </div>
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
          title="New field"
          farmId={fieldForm.farmId}
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
          title="New zone"
          farmId={zoneForm.fieldId}
          farms={farms}
          fields={fields}
          form={zoneForm}
          setForm={setZoneForm}
          onCancel={() => setShowZoneForm(false)}
          onSave={saveZone}
          busy={busy}
        />
      )}

      {deleteTarget && (
        <ConfirmDialog
          title={`Delete ${deleteTarget.title}`}
          message="Do you really want to delete this item? This action cannot be undone."
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
          busy={busy}
        />
      )}
    </div>
  )
}
/* ===== Farm form ===== */
function FarmFormModal({ editing, form, setForm, onCancel, onSave, busy, geo, onLocate, formError }) {
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <ModalShell onCancel={onCancel}>
      <h3 style={modalTitle}>{editing ? 'Edit farm' : 'New farm'}</h3>
      {formError && <div className="ws-inline-error">{formError}</div>}
      <form onSubmit={onSave} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <FormField label="Name" required>
          <input style={inputStyle} value={form.name} onChange={field('name')} required />
        </FormField>
        <FormField label="Address">
          <input style={inputStyle} value={form.address} onChange={field('address')} />
        </FormField>
        <FormField label="Area (ha)">
          <input style={inputStyle} type="number" step="0.01" value={form.areaHectares} onChange={field('areaHectares')} />
        </FormField>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
          <FormField label="Latitude">
            <input style={inputStyle} type="number" step="any" min="-90" max="90" value={form.latitude} onChange={field('latitude')} />
          </FormField>
          <FormField label="Longitude">
            <input style={inputStyle} type="number" step="any" min="-180" max="180" value={form.longitude} onChange={field('longitude')} />
          </FormField>
        </div>
        <LocateHint geo={geo} onLocate={onLocate} />
        <FormField label="Description">
          <textarea style={{ ...inputStyle, minHeight: '70px', resize: 'vertical' }} value={form.description} onChange={field('description')} />
        </FormField>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' }}>
          <button type="button" style={btn('ghost')} onClick={onCancel}>Cancel</button>
          <button type="submit" style={btn('primary')} disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </ModalShell>
  )

}

/* ===== Field / Zone form ===== */
function ChildFormModal({ title, farms, fields, form, setForm, onCancel, onSave, busy, geo, onLocate, formError }) {
  const isZone = 'fieldId' in form
  const parentKey = isZone ? 'fieldId' : 'farmId'
  const parents = isZone ? fields : farms
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <ModalShell onCancel={onCancel}>
      <h3 style={modalTitle}>{title}</h3>
      {formError && <div className="ws-inline-error">{formError}</div>}
      <form onSubmit={onSave} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <FormField label={isZone ? 'Parent field' : 'Parent farm'} required>
          <select style={inputStyle} value={form[parentKey]} onChange={field(parentKey)} required>
            <option value="">-- Select --</option>
            {parents.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </FormField>
        <FormField label="Name" required>
          <input style={inputStyle} value={form.name} onChange={field('name')} required />
        </FormField>
        {isZone && (
          <FormField label="Irrigation method">
            <select style={inputStyle} value={form.irrigationMethod} onChange={field('irrigationMethod')}>
              <option value="">-- Select --</option>
              <option value="drip">Drip</option>
              <option value="sprinkler">Sprinkler</option>
              <option value="surface">Surface</option>
            </select>
          </FormField>
        )}
        {!isZone && (
          <FormField label="Crop type">
            <input style={inputStyle} value={form.cropType} onChange={field('cropType')} />
          </FormField>
        )}
        <FormField label="Area (ha)" required>
          <input style={inputStyle} type="number" step="0.01" value={form.areaHectares} onChange={field('areaHectares')} required />
        </FormField>
        {!isZone && (
          <>
            <FormField label="Location (latitude, longitude)">
              <input style={inputStyle} value={form.coordinates} onChange={field('coordinates')} placeholder="10.633100, -4.761200" />
            </FormField>
            <LocateHint geo={geo} onLocate={onLocate} />
          </>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' }}>
          <button type="button" style={btn('ghost')} onClick={onCancel}>Cancel</button>
          <button type="submit" style={btn('primary')} disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </ModalShell>
  )
}

/** Geolocation state under the coordinates: locating, accuracy obtained, or why it failed. */
function LocateHint({ geo, onLocate }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap', fontSize: '12px', color: 'var(--ws-muted, #6b7a78)' }}>
      <button type="button" style={btn('ghost')} onClick={onLocate} disabled={geo.locating}>
        <LocateFixed size={14} /> {geo.locating ? 'Locating…' : 'Use my position'}
      </button>
      {geo.error ? (
        <span style={{ color: 'var(--ws-red, #c62828)' }}>{geo.error}</span>
      ) : geo.accuracy != null ? (
        <span>Position of this device, accurate to about {geo.accuracy} m: correct it if you are not on site.</span>
      ) : null}
    </div>
  )
}

/* ===== Modal shell ===== */
function ModalShell({ onCancel, children }) {
  return (
    <div
      style={{ position: 'fixed', inset: 0, zIndex: 40, display: 'grid', placeItems: 'center', background: 'rgba(16,47,43,.4)', padding: '20px', fontFamily: "'Inter', Arial, sans-serif" }}
      onClick={onCancel}
    >
      <div
        style={{ width: '100%', maxWidth: '460px', background: 'var(--ws-card,#fff)', border: '1px solid var(--ws-line,#e2e9e7)', borderRadius: '12px', padding: '24px', boxShadow: '0 16px 40px rgba(0,0,0,.18)', maxHeight: '90vh', overflow: 'auto' }}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  )
}

const modalTitle = { margin: '0 0 16px', fontFamily: "'Montserrat', Arial, sans-serif", fontWeight: 600, fontSize: '17px', color: 'var(--ws-ink,#1c2b29)' }
