import { useCallback, useEffect, useMemo, useState } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { farmApi, fieldApi, zoneApi } from '../../api/watersupplyApi'
import { DataTable } from '../../../../shared/components/DataTable'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'
import { EmptyState } from '../../../../shared/components/EmptyState'
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

/**
 * CRUD view for Farms / Fields / Zones (Task 4.1).
 * Uses the reusable design-system components.
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
  const [farmForm, setFarmForm] = useState({ name: '', address: '', areaHectares: '', description: '' })
  const [deleteTarget, setDeleteTarget] = useState(null) // {kind, id, title}
  const [busy, setBusy] = useState(false)

  const [showFieldForm, setShowFieldForm] = useState(false)
  const [fieldForm, setFieldForm] = useState({ farmId: '', name: '', areaHectares: '', cropType: '', soilType: '' })
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

  const openFarmForm = (farm) => {
    if (farm) {
      setEditingFarm(farm)
      setFarmForm({ name: farm.name || '', address: farm.address || '', areaHectares: farm.areaHectares ?? '', description: farm.description || '' })
    } else {
      setEditingFarm(null)
      setFarmForm({ name: '', address: '', areaHectares: '', description: '' })
    }
    setShowFarmForm(true)
  }

  const saveFarm = (e) => {
    e.preventDefault()
    setBusy(true)
    const payload = {
      name: farmForm.name,
      address: farmForm.address || null,
      areaHectares: farmForm.areaHectares === '' ? null : Number(farmForm.areaHectares),
      description: farmForm.description || null,
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
    setBusy(true)
    const payload = {
      farmId: fieldForm.farmId,
      name: fieldForm.name,
      areaHectares: fieldForm.areaHectares === '' ? 0 : Number(fieldForm.areaHectares),
      cropType: fieldForm.cropType || null,
      soilType: fieldForm.soilType || null,
    }
    fieldApi
      .createField(payload)
      .then(() => {
        notify('Field created')
        setShowFieldForm(false)
        setFieldForm((p) => ({ ...p, name: '', areaHectares: '', cropType: '', soilType: '' }))
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
              <button style={btn('primary')} onClick={() => setShowFieldForm(true)}>
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
function FarmFormModal({ editing, form, setForm, onCancel, onSave, busy }) {
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <ModalShell onCancel={onCancel}>
      <h3 style={modalTitle}>{editing ? 'Edit farm' : 'New farm'}</h3>
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
function ChildFormModal({ title, farms, fields, form, setForm, onCancel, onSave, busy }) {
  const isZone = 'fieldId' in form
  const parentKey = isZone ? 'fieldId' : 'farmId'
  const parents = isZone ? fields : farms
  const field = (k) => (e) => setForm((p) => ({ ...p, [k]: e.target.value }))
  return (
    <ModalShell onCancel={onCancel}>
      <h3 style={modalTitle}>{title}</h3>
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
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' }}>
          <button type="button" style={btn('ghost')} onClick={onCancel}>Cancel</button>
          <button type="submit" style={btn('primary')} disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </ModalShell>
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
