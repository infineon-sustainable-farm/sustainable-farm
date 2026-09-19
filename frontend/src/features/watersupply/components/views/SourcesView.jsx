import { useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { waterSourceApi } from '../../api/watersupplyApi'
import { useFarms } from '../../hooks/useFarms'
import { useWaterSources } from '../../hooks/useWaterData'
import { useListControls } from '../../../../shared/hooks/useListControls'
import { SearchInput } from '../../../../shared/components/SearchInput'
import { Pagination } from '../../../../shared/components/Pagination'
import { Spinner } from '../../../../shared/components/Spinner'
import { EmptyState } from '../../../../shared/components/EmptyState'
import { FormField } from '../../../../shared/components/FormField'
import { inputStyle } from '../../../../shared/components/formStyles'
import { ConfirmDialog } from '../../../../shared/components/ConfirmDialog'

const SOURCE_TYPES = ['borehole', 'rain', 'river', 'well', 'municipal']

/**
 * Water sources view: full CRUD with farm association and validation.
 * Backend contract: WaterSource { farmId, name, type, capacityLiters, currentLevelLiters }.
 */
export function SourcesView({ notify }) {
  const { sources, loading, error, refetch } = useWaterSources()
  const { farms } = useFarms()
  const [farmFilter, setFarmFilter] = useState('')
  const [form, setForm] = useState({ farmId: '', name: '', type: 'borehole', capacityLiters: '', currentLevelLiters: '' })
  const [editingId, setEditingId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [busy, setBusy] = useState(false)
  const [formError, setFormError] = useState(null)

  const farmName = (farmId) => farms.find((f) => f.id === farmId)?.name || (farmId ? `Farm ${String(farmId).slice(0, 8)}` : '—')

  const farmFiltered = useMemo(
    () => sources.filter((item) => !farmFilter || item.farmId === farmFilter),
    [sources, farmFilter],
  )

  // Client-side search + sort + pagination.
  const list = useListControls(farmFiltered, {
    searchFields: ['name', 'type'],
    defaultSort: { key: 'name', dir: 'asc' },
  })

  const updateForm = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const resetForm = () => {
    setEditingId(null)
    setForm({ farmId: '', name: '', type: 'borehole', capacityLiters: '', currentLevelLiters: '' })
    setFormError(null)
  }

  const submitSource = (event) => {
    event.preventDefault()
    if (!form.farmId) {
      setFormError('The farm is required.')
      return
    }
    if (!form.name.trim()) {
      setFormError('The name is required.')
      return
    }
    const capacity = Number(form.capacityLiters)
    if (!(capacity > 0)) {
      setFormError('The capacity must be greater than 0.')
      return
    }
    const level = form.currentLevelLiters === '' ? 0 : Number(form.currentLevelLiters)
    if (level < 0 || level > capacity) {
      setFormError('The current level must be between 0 and the capacity.')
      return
    }
    setBusy(true)
    setFormError(null)
    const payload = {
      farmId: form.farmId,
      name: form.name.trim(),
      type: form.type,
      capacityLiters: capacity,
      currentLevelLiters: level,
    }
    const request = editingId ? waterSourceApi.updateSource(editingId, payload) : waterSourceApi.createSource(payload)
    request
      .then(() => refetch())
      .then(() => {
        notify(editingId ? 'Source updated' : 'Source created')
        resetForm()
      })
      .catch((err) => setFormError(err.message || 'Unable to save.'))
      .finally(() => setBusy(false))
  }

  const editSource = (item) => {
    setEditingId(item.id)
    setForm({
      farmId: item.farmId || '',
      name: item.name || '',
      type: item.type || 'borehole',
      capacityLiters: item.capacityLiters ?? '',
      currentLevelLiters: item.currentLevelLiters ?? '',
    })
    setFormError(null)
  }

  const deleteSource = () => {
    if (!deleteTarget) return
    setBusy(true)
    waterSourceApi
      .deleteSource(deleteTarget.id)
      .then(() => refetch())
      .then(() => {
        notify('Source deleted')
        setDeleteTarget(null)
      })
      .catch((err) => setFormError(err.message || 'Unable to delete.'))
      .finally(() => setBusy(false))
  }

  return (
    <>
      <div className="ws-topbar">
        <div className="ws-title-block">
          <h1>Water sources</h1>
          <p>Boreholes, rain tanks, rivers: manage abstraction points per farm.</p>
        </div>
      </div>

      <div className="ws-layout-2">
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>Source list</h2>
            <span>{list.total} source(s)</span>
          </div>
          <div className="ws-filters">
            <select value={farmFilter} onChange={(event) => setFarmFilter(event.target.value)}>
              <option value="">All farms</option>
              {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
            </select>
            <SearchInput value={list.query} onChange={list.setQuery} placeholder="Search a source…" />
          </div>
          <div className="ws-panel-body">
            {loading ? (
              <Spinner label="Loading sources..." full />
            ) : error ? (
              <EmptyState title="Error" description={error.message || 'Unable to load sources.'} />
            ) : farmFiltered.length === 0 ? (
              <EmptyState title="No sources" description="Create a water source linked to a farm to get started." />
            ) : (
              <table className="ws-table">
                <thead>
                  <tr>
                    <th onClick={() => list.toggleSort('name')} style={{ cursor: 'pointer' }}>
                      Name {list.sort?.key === 'name' ? (list.sort.dir === 'asc' ? '↑' : '↓') : ''}
                    </th>
                    <th>Farm</th>
                    <th>Type</th>
                    <th>Capacity</th>
                    <th>Level</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {list.items.map((item) => {
                    const pct = item.capacityLiters ? Math.round((item.currentLevelLiters / item.capacityLiters) * 100) : 0
                    return (
                      <tr key={item.id}>
                        <td><strong>{item.name}</strong></td>
                        <td>{farmName(item.farmId)}</td>
                        <td><span className="ws-tag primary">{item.type || '—'}</span></td>
                        <td>{item.capacityLiters ?? 0} L</td>
                        <td>
                          <span className={`ws-tag ${pct >= 90 ? 'orange' : pct > 0 ? 'green' : 'red'}`}>{item.currentLevelLiters ?? 0} L ({pct}%)</span>
                        </td>
                        <td className="ws-table-actions">
                          <button className="ws-chip" onClick={() => editSource(item)}>Edit</button>
                          <button className="ws-icon-btn danger" title="Delete" onClick={() => setDeleteTarget(item)}>
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
            <Pagination page={list.page} pageCount={list.pageCount} onPage={list.setPage} total={list.total} unit="source" />
          </div>
        </div>
        <div className="ws-panel">
          <div className="ws-panel-header">
            <h2>{editingId ? 'Edit source' : 'New source'}</h2>
            <Plus size={16} />
          </div>
          <div className="ws-panel-body">
            {formError && <div className="ws-inline-error">{formError}</div>}
            <form className="ws-form-grid" onSubmit={submitSource}>
              <FormField label="Farm" required>
                <select style={inputStyle} value={form.farmId} onChange={updateForm('farmId')} required>
                  <option value="">Select</option>
                  {farms.map((farm) => <option key={farm.id} value={farm.id}>{farm.name}</option>)}
                </select>
              </FormField>
              <FormField label="Name" required>
                <input style={inputStyle} value={form.name} onChange={updateForm('name')} required />
              </FormField>
              <FormField label="Type" required>
                <select style={inputStyle} value={form.type} onChange={updateForm('type')} required>
                  {SOURCE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                </select>
              </FormField>
              <FormField label="Capacity (L)" required>
                <input style={inputStyle} type="number" min="0" step="0.01" value={form.capacityLiters} onChange={updateForm('capacityLiters')} required />
              </FormField>
              <div className="ws-form-actions">
                {editingId && <button type="button" className="ws-chip" onClick={resetForm}>Cancel</button>}
                <button type="submit" className="ws-action-btn" disabled={busy}>{busy ? '...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      </div>
      {deleteTarget && (
        <ConfirmDialog
          title={`Delete ${deleteTarget.name || 'the source'}`}
          message="This source will be removed. Related consumption and quality tests could be impacted."
          onConfirm={deleteSource}
          onCancel={() => setDeleteTarget(null)}
          busy={busy}
        />
      )}
    </>
  )
}