/**
 * Product Transformation - Equipment List Component
 * Displays, filters, creates, edits and deletes PT-only equipment.
 *
 * Equipment collision rule (Option A): this screen manages `pt_equipment`
 * (/api/equipment), not the Machinery module's `equipment`.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { equipmentApi } from '../api/equipmentApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { formatIsoDate, formatNumber } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { Plus, Filter, Pencil, Trash2, Search } from 'lucide-react';

import { CARD_FRAME, FIELD_INPUT, FIELD_LABEL, FILTER_INPUT, PAGE_TITLE, PRIMARY_BUTTON, SEARCH_INPUT, SECONDARY_BUTTON, SECTION_CARD, TABLE_CARD, TABLE_HEAD_BASE } from '../styles/ptStyles';

const EQUIPMENT_TYPES = [
  'WASHING',
  'DRYING',
  'PACKAGING',
  'TESTA_DRYER',
  'SOLAR_DRYER',
  'TUNNEL_DRYER',
];

const MAINTENANCE_STATUSES = ['ACTIVE', 'MAINTENANCE', 'RETIRED'];

const EQUIPMENT_TYPE_LABELS = {
  WASHING: 'Washing',
  DRYING: 'Drying',
  PACKAGING: 'Packaging',
  TESTA_DRYER: 'Testa dryer',
  SOLAR_DRYER: 'Solar dryer',
  TUNNEL_DRYER: 'Tunnel dryer',
};

const equipmentTypeLabel = (value) => EQUIPMENT_TYPE_LABELS[value] || value || 'N/A';

const STATUS_BADGES = {
  ACTIVE: 'bg-green-100 text-green-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
  RETIRED: 'bg-gray-100 text-gray-700',
};

const EMPTY_FILTERS = {
  type: 'all',
  status: 'all',
  search: '',
};

const emptyForm = () => ({
  equipmentId: '',
  equipmentName: '',
  equipmentType: 'WASHING',
  capacityKgPerHour: '',
  energyConsumptionKwhPerKg: '',
  location: '',
  maintenanceStatus: 'ACTIVE',
  lastMaintenanceDate: '',
});

const EquipmentList = () => {
  const [equipment, setEquipment] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [actionError, setActionError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await equipmentApi.getAllEquipment();
      setEquipment(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const filteredEquipment = useMemo(() => {
    const search = filters.search.trim().toLowerCase();
    return equipment
      .filter((item) => {
        if (filters.type !== 'all' && item.equipmentType !== filters.type) return false;
        if (filters.status !== 'all' && item.maintenanceStatus !== filters.status) return false;
        if (
          search &&
          !`${item.equipmentId} ${item.equipmentName} ${item.location || ''}`
            .toLowerCase()
            .includes(search)
        ) {
          return false;
        }
        return true;
      })
      .sort((a, b) => String(a.equipmentId).localeCompare(String(b.equipmentId)));
  }, [equipment, filters]);

  const updateFilter = (key) => (event) => {
    setFilters((current) => ({ ...current, [key]: event.target.value }));
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm());
    setFormError('');
    setActionError('');
    setShowForm(true);
  };

  const openEdit = (item) => {
    setEditing(item);
    setForm({
      equipmentId: item.equipmentId || '',
      equipmentName: item.equipmentName || '',
      equipmentType: item.equipmentType || 'WASHING',
      capacityKgPerHour: String(item.capacityKgPerHour ?? ''),
      energyConsumptionKwhPerKg: String(item.energyConsumptionKwhPerKg ?? ''),
      location: item.location || '',
      maintenanceStatus: item.maintenanceStatus || 'ACTIVE',
      lastMaintenanceDate: item.lastMaintenanceDate
        ? String(item.lastMaintenanceDate).slice(0, 10)
        : '',
    });
    setFormError('');
    setActionError('');
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;
    setShowForm(false);
    setEditing(null);
    setFormError('');
  };

  const updateForm = (key) => (event) => {
    setForm((current) => ({ ...current, [key]: event.target.value }));
  };

  const validate = () => {
    if (!editing && !form.equipmentId.trim()) return 'Equipment ID is required';
    if (!form.equipmentName.trim()) return 'Equipment name is required';
    if (!form.equipmentType) return 'Equipment type is required';
    if (form.capacityKgPerHour === '' || Number(form.capacityKgPerHour) <= 0) {
      return 'Capacity must be greater than 0';
    }
    if (
      form.energyConsumptionKwhPerKg === '' ||
      Number(form.energyConsumptionKwhPerKg) <= 0
    ) {
      return 'Energy consumption must be greater than 0';
    }
    return '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      const payload = {
        equipmentName: form.equipmentName.trim(),
        equipmentType: form.equipmentType,
        capacityKgPerHour: Number(form.capacityKgPerHour),
        energyConsumptionKwhPerKg: Number(form.energyConsumptionKwhPerKg),
        location: form.location.trim() || null,
        maintenanceStatus: form.maintenanceStatus,
        lastMaintenanceDate: form.lastMaintenanceDate || null,
      };

      if (editing) {
        await equipmentApi.updateEquipment(editing.equipmentId, payload);
      } else {
        await equipmentApi.createEquipment({
          equipmentId: form.equipmentId.trim(),
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the equipment');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await equipmentApi.deleteEquipment(deleteTarget.equipmentId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the equipment');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <LoadingState message="Loading equipment..." />;
  if (error)
    return <ErrorState message={error?.message || 'Failed to load equipment'} onRetry={load} />;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className={PAGE_TITLE}>Equipment</h1>
          <p className="text-sm text-gray-600 mt-1">
            Product Transformation equipment: capacity, energy use and maintenance status.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className={PRIMARY_BUTTON}
        >
          <Plus className="w-4 h-4" />
          New Equipment
        </button>
      </div>

      {actionError && (
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {actionError}
        </div>
      )}

      {/* Filters */}
      <div className={`${SECTION_CARD} mb-6`}>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex items-center gap-2 text-gray-500">
            <Filter className="w-5 h-5" />
            <span className="text-sm font-medium">Filters</span>
          </div>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Type
            <select
              aria-label="Filter by type"
              value={filters.type}
              onChange={updateFilter('type')}
              className={FILTER_INPUT}
            >
              <option value="all">All types</option>
              {EQUIPMENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {equipmentTypeLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Status
            <select
              aria-label="Filter by status"
              value={filters.status}
              onChange={updateFilter('status')}
              className={FILTER_INPUT}
            >
              <option value="all">All statuses</option>
              {MAINTENANCE_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {formatEnumLabel(status)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Search
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                aria-label="Search equipment"
                placeholder="ID, name or location"
                value={filters.search}
                onChange={updateFilter('search')}
                className={SEARCH_INPUT}
              />
            </div>
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredEquipment.length} equipment
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredEquipment.length === 0 ? (
        <div className={CARD_FRAME}>
          <EmptyState
            message={equipment.length === 0 ? 'No equipment found' : 'No equipment matches the filters'}
            actionLabel={equipment.length === 0 ? 'New Equipment' : undefined}
            onAction={equipment.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                {[
                  'Equipment',
                  'Name',
                  'Type',
                  'Capacity',
                  'Energy',
                  'Location',
                  'Status',
                  'Last Maintenance',
                  'Actions',
                ].map((label) => (
                  <th
                    key={label}
                    className={`${TABLE_HEAD_BASE} ${label === 'Actions' ? 'text-right' : 'text-left'}`}
                  >
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredEquipment.map((item) => (
                <tr key={item.equipmentId} className="hover:bg-gray-50">
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{item.equipmentId}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {item.equipmentName}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {equipmentTypeLabel(item.equipmentType)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatNumber(item.capacityKgPerHour, 1)} kg/h
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatNumber(item.energyConsumptionKwhPerKg, 2)} kWh/kg
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {item.location || 'N/A'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        STATUS_BADGES[item.maintenanceStatus] || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {formatEnumLabel(item.maintenanceStatus || 'UNKNOWN')}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatIsoDate(item.lastMaintenanceDate)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="inline-flex items-center gap-1">
                      <button
                        type="button"
                        aria-label="Edit"
                        title="Edit"
                        onClick={() => openEdit(item)}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-md text-primary hover:bg-primary/10 hover:text-primary/80"
                      >
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        aria-label="Delete"
                        title="Delete"
                        onClick={() => setDeleteTarget(item)}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-md text-red-600 hover:bg-red-50 hover:text-red-700"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit modal */}
      {showForm && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={editing ? 'Edit equipment' : 'New equipment'}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-2xl p-6 shadow-card w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="font-heading text-xl font-bold text-on-background mb-4">
              {editing ? 'Edit Equipment' : 'New Equipment'}
            </h2>

            {formError && (
              <div
                role="alert"
                className="mb-4 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700"
              >
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Equipment ID
                  <input
                    type="text"
                    value={form.equipmentId}
                    onChange={updateForm('equipmentId')}
                    disabled={Boolean(editing)}
                    className={`${FIELD_INPUT} disabled:bg-gray-100`}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Name
                  <input
                    type="text"
                    value={form.equipmentName}
                    onChange={updateForm('equipmentName')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Type
                  <select
                    value={form.equipmentType}
                    onChange={updateForm('equipmentType')}
                    className={FIELD_INPUT}
                  >
                    {EQUIPMENT_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {equipmentTypeLabel(type)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Maintenance Status
                  <select
                    value={form.maintenanceStatus}
                    onChange={updateForm('maintenanceStatus')}
                    className={FIELD_INPUT}
                  >
                    {MAINTENANCE_STATUSES.map((status) => (
                      <option key={status} value={status}>
                        {formatEnumLabel(status)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Capacity (kg/h)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.capacityKgPerHour}
                    onChange={updateForm('capacityKgPerHour')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Energy (kWh/kg)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.energyConsumptionKwhPerKg}
                    onChange={updateForm('energyConsumptionKwhPerKg')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Location
                  <input
                    type="text"
                    value={form.location}
                    onChange={updateForm('location')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Last Maintenance
                  <input
                    type="date"
                    value={form.lastMaintenanceDate}
                    onChange={updateForm('lastMaintenanceDate')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={saving}
                  className={SECONDARY_BUTTON}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className={PRIMARY_BUTTON}
                >
                  {saving ? 'Saving...' : editing ? 'Save' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete equipment"
          message={`Delete equipment ${deleteTarget.equipmentId}? This action cannot be undone.`}
          confirmLabel="Delete"
          busy={deleting}
          onConfirm={handleDelete}
          onCancel={() => {
            if (!deleting) setDeleteTarget(null);
          }}
        />
      )}
    </div>
  );
};

export default EquipmentList;
