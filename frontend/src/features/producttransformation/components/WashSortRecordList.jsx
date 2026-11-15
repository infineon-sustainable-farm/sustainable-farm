/**
 * Product Transformation - Wash & Sort Record List Component
 * Displays, filters, creates, edits and deletes washing/sorting records.
 *
 * Data contract: /api/wash-sort-records (see WashSortRecordController).
 * The backend rejects non-positive input/output quantities, negative waste or
 * water usage, inactive equipment and operators without the WASHER role.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { washSortApi } from '../api/washSortApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { formatWeight, formatNumber, formatDateTime } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { Plus, Filter, Pencil, Trash2 } from 'lucide-react';

const EQUIPMENT_TYPE = 'WASHING';

/** Convert an API LocalDateTime string to a datetime-local input value. */
const toDateTimeLocal = (value) => {
  if (!value) return '';
  return String(value).slice(0, 16).replace(' ', 'T');
};

/** Convert a datetime-local input value to the API LocalDateTime format. */
const fromDateTimeLocal = (value) => {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
};

const EMPTY_FILTERS = {
  batchId: 'all',
  equipmentId: 'all',
  date: '',
};

const emptyForm = () => ({
  recordId: '',
  batchId: '',
  inputQuantityKg: '',
  outputQuantityKg: '',
  wasteQuantityKg: '',
  waterUsageLiters: '',
  startTime: '',
  endTime: '',
  equipmentId: '',
  operatorId: '',
});

const WashSortRecordList = () => {
  const [records, setRecords] = useState([]);
  const [batches, setBatches] = useState([]);
  const [equipment, setEquipment] = useState([]);
  const [operators, setOperators] = useState([]);
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
      const [recordData, batchData, equipmentData, operatorData] = await Promise.all([
        washSortApi.getAllWashSortRecords(),
        batchApi.getAllBatches(),
        equipmentApi.getAllEquipment(),
        operatorApi.getAllOperators(),
      ]);
      setRecords(Array.isArray(recordData) ? recordData : []);
      setBatches(Array.isArray(batchData) ? batchData : []);
      setEquipment(Array.isArray(equipmentData) ? equipmentData : []);
      setOperators(Array.isArray(operatorData) ? operatorData : []);
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

  const washEquipment = useMemo(
    () => equipment.filter((item) => item.equipmentType === EQUIPMENT_TYPE),
    [equipment],
  );

  // Every operator is used to resolve names on existing records; only WASHERs
  // can be assigned to a new record (the backend enforces that rule).
  const washOperators = useMemo(
    () => operators.filter((operator) => operator.role === 'WASHER'),
    [operators],
  );

  const filteredRecords = useMemo(() => {
    return records
      .filter((record) => {
        if (filters.batchId !== 'all' && record.batchId !== filters.batchId) return false;
        if (filters.equipmentId !== 'all' && record.equipmentId !== filters.equipmentId)
          return false;
        if (filters.date && String(record.startTime || '').slice(0, 10) !== filters.date)
          return false;
        return true;
      })
      .sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || '')));
  }, [records, filters]);

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

  const openEdit = (record) => {
    setEditing(record);
    setForm({
      recordId: record.recordId || '',
      batchId: record.batchId || '',
      inputQuantityKg: String(record.inputQuantityKg ?? ''),
      outputQuantityKg: String(record.outputQuantityKg ?? ''),
      wasteQuantityKg: String(record.wasteQuantityKg ?? ''),
      waterUsageLiters: String(record.waterUsageLiters ?? ''),
      startTime: toDateTimeLocal(record.startTime),
      endTime: toDateTimeLocal(record.endTime),
      equipmentId: record.equipmentId || '',
      operatorId: record.operatorId || '',
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
    if (!editing && !form.recordId.trim()) return 'Record ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (form.inputQuantityKg === '' || Number(form.inputQuantityKg) <= 0) {
      return 'Input quantity must be greater than 0';
    }
    if (form.outputQuantityKg === '' || Number(form.outputQuantityKg) <= 0) {
      return 'Output quantity must be greater than 0';
    }
    if (form.wasteQuantityKg === '' || Number(form.wasteQuantityKg) < 0) {
      return 'Waste quantity cannot be negative';
    }
    if (form.waterUsageLiters === '' || Number(form.waterUsageLiters) < 0) {
      return 'Water usage cannot be negative';
    }
    if (!form.startTime) return 'Start time is required';
    if (!form.endTime) return 'End time is required';
    if (new Date(form.endTime) <= new Date(form.startTime)) {
      return 'End time must be after start time';
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
        inputQuantityKg: Number(form.inputQuantityKg),
        outputQuantityKg: Number(form.outputQuantityKg),
        wasteQuantityKg: Number(form.wasteQuantityKg),
        waterUsageLiters: Number(form.waterUsageLiters),
        startTime: fromDateTimeLocal(form.startTime),
        endTime: fromDateTimeLocal(form.endTime),
        equipmentId: form.equipmentId || null,
        operatorId: form.operatorId || null,
      };

      if (editing) {
        await washSortApi.updateWashSortRecord(editing.recordId, payload);
      } else {
        await washSortApi.createWashSortRecord({
          recordId: form.recordId.trim(),
          batchId: form.batchId,
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the wash & sort record');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await washSortApi.deleteWashSortRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the wash & sort record');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <LoadingState message="Loading wash & sort records..." />;
  if (error)
    return (
      <ErrorState
        message={error?.message || 'Failed to load wash & sort records'}
        onRetry={load}
      />
    );

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Washing &amp; Sorting</h1>
          <p className="text-sm text-gray-600 mt-1">
            Track washing yields, waste and water usage per batch.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
        >
          <Plus className="w-4 h-4" />
          New Wash Record
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
      <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex items-center gap-2 text-gray-500">
            <Filter className="w-5 h-5" />
            <span className="text-sm font-medium">Filters</span>
          </div>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Batch
            <select
              aria-label="Filter by batch"
              value={filters.batchId}
              onChange={updateFilter('batchId')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All batches</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Equipment
            <select
              aria-label="Filter by equipment"
              value={filters.equipmentId}
              onChange={updateFilter('equipmentId')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All equipment</option>
              {washEquipment.map((item) => (
                <option key={item.equipmentId} value={item.equipmentId}>
                  {item.equipmentName}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Start Date
            <input
              type="date"
              aria-label="Filter by start date"
              value={filters.date}
              onChange={updateFilter('date')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredRecords.length} records
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredRecords.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm">
          <EmptyState
            message={records.length === 0 ? 'No wash & sort records found' : 'No records match the filters'}
            actionLabel={records.length === 0 ? 'New Wash Record' : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                {[
                  'Record',
                  'Batch',
                  'Quantities',
                  'Yield',
                  'Water',
                  'Period',
                  'Equipment / Operator',
                  'Actions',
                ].map((label) => (
                  <th
                    key={label}
                    className={`px-3 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider ${
                      label === 'Actions' ? 'text-right' : 'text-left'
                    }`}
                  >
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredRecords.map((record) => {
                const equipmentLabel =
                  equipment.find((item) => item.equipmentId === record.equipmentId)
                    ?.equipmentName || record.equipmentId || 'N/A';
                const operatorLabel =
                  operators.find((item) => item.operatorId === record.operatorId)
                    ?.operatorName || record.operatorId || 'N/A';

                return (
                  <tr key={record.recordId} className="hover:bg-gray-50">
                    <td className="px-3 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{record.recordId}</div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {record.batchId}
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div>
                        {formatWeight(record.outputQuantityKg)} / {formatWeight(record.inputQuantityKg)}
                      </div>
                      <div className="text-xs text-gray-500">
                        Waste {formatWeight(record.wasteQuantityKg)}
                      </div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {record.yieldPercentage === null || record.yieldPercentage === undefined
                        ? 'N/A'
                        : `${formatNumber(record.yieldPercentage, 1)}%`}
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-600">
                      {record.waterUsageLiters === null || record.waterUsageLiters === undefined
                        ? 'N/A'
                        : `${formatNumber(record.waterUsageLiters, 0)} L`}
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-600">
                      <div>{formatDateTime(record.startTime)}</div>
                      <div className="text-xs text-gray-500">to {formatDateTime(record.endTime)}</div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-600">
                      <div className="text-gray-900">{equipmentLabel}</div>
                      <div className="text-xs text-gray-500">{operatorLabel}</div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="inline-flex items-center gap-1">
                        <button
                          type="button"
                          aria-label="Edit"
                          title="Edit"
                          onClick={() => openEdit(record)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-primary hover:bg-primary/10 hover:text-primary/80"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          aria-label="Delete"
                          title="Delete"
                          onClick={() => setDeleteTarget(record)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-red-600 hover:bg-red-50 hover:text-red-700"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit modal */}
      {showForm && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={editing ? 'Edit wash & sort record' : 'New wash & sort record'}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-lg p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              {editing ? 'Edit Wash & Sort Record' : 'New Wash & Sort Record'}
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
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Record ID
                  <input
                    type="text"
                    value={form.recordId}
                    onChange={updateForm('recordId')}
                    disabled={Boolean(editing)}
                    className="border border-gray-300 rounded-lg px-3 py-2 disabled:bg-gray-100"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Batch
                  <select
                    value={form.batchId}
                    onChange={updateForm('batchId')}
                    disabled={Boolean(editing)}
                    className="border border-gray-300 rounded-lg px-3 py-2 disabled:bg-gray-100"
                  >
                    <option value="">Select a batch</option>
                    {batches.map((batch) => (
                      <option key={batch.batchId} value={batch.batchId}>
                        {batch.batchId}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Input (kg)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.inputQuantityKg}
                    onChange={updateForm('inputQuantityKg')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Output (kg)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.outputQuantityKg}
                    onChange={updateForm('outputQuantityKg')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Waste (kg)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.wasteQuantityKg}
                    onChange={updateForm('wasteQuantityKg')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Water (L)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.waterUsageLiters}
                    onChange={updateForm('waterUsageLiters')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Start Time
                  <input
                    type="datetime-local"
                    value={form.startTime}
                    onChange={updateForm('startTime')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  End Time
                  <input
                    type="datetime-local"
                    value={form.endTime}
                    onChange={updateForm('endTime')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Equipment
                  <select
                    value={form.equipmentId}
                    onChange={updateForm('equipmentId')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    <option value="">No equipment</option>
                    {washEquipment.map((item) => (
                      <option key={item.equipmentId} value={item.equipmentId}>
                        {item.equipmentName} · {formatEnumLabel(item.maintenanceStatus || 'ACTIVE')}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Operator
                  <select
                    value={form.operatorId}
                    onChange={updateForm('operatorId')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    <option value="">No operator</option>
                    {washOperators.map((operator) => (
                      <option key={operator.operatorId} value={operator.operatorId}>
                        {operator.operatorName}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={saving}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 disabled:opacity-60"
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
          title="Delete wash & sort record"
          message={`Delete record ${deleteTarget.recordId}? This action cannot be undone.`}
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

export default WashSortRecordList;
