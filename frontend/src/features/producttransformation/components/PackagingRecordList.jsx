/**
 * Product Transformation - Packaging Record List Component
 * Displays, filters, creates, edits and deletes packaging records.
 *
 * Data contract: /api/packaging-records (see PackagingRecordController).
 * Business rules enforced by the backend: lot codes are mandatory and unique,
 * quantity must be positive, and export readiness is tracked per record.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { packagingApi } from '../api/packagingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useAccessibleDialog } from '../../../shared/hooks/useAccessibleDialog';
import { formatWeight, formatNumber } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import { Plus, Filter, Pencil, Trash2, Link2, PackageCheck, Sparkles } from 'lucide-react';

import PTListHeader from './PTListHeader';
import { CARD, CARD_FRAME, FIELD_INPUT, FIELD_LABEL, FILTER_INPUT, PAGE_TITLE, PRIMARY_BUTTON, SECONDARY_BUTTON, SECTION_CARD, TABLE_CARD, TABLE_HEAD_BASE } from '../styles/ptStyles';

const PACKAGE_TYPES = ['ONE_KG_BAG', 'TWO_KG_BAG', 'BULK'];

const PACKAGE_LABELS = {
  ONE_KG_BAG: '1 kg bag',
  TWO_KG_BAG: '2 kg bag',
  BULK: 'Bulk',
};

const packageLabel = (value) => PACKAGE_LABELS[value] || (value ? formatEnumLabel(value) : '—');

/** Date-only values (YYYY-MM-DD) must not shift across time zones. */
const formatIsoDate = (value) => {
  if (!value) return 'N/A';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  if (!year || !month || !day) return String(value);
  return new Date(Number(year), Number(month) - 1, Number(day)).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Suggest an editable, traceability-friendly lot code.
 * Uniqueness is still enforced by the backend, so this only fills the field.
 */
const suggestLotCode = (batchId, packagingDate) => {
  const now = new Date();
  const time = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(
    2,
    '0',
  )}${String(now.getSeconds()).padStart(2, '0')}`;
  const date = String(packagingDate || todayIsoDate()).replace(/-/g, '');
  return `LOT-${(batchId || 'BATCH').toUpperCase()}-${date}-${time}`;
};

const EMPTY_FILTERS = {
  batchId: 'all',
  packageType: 'all',
  exportReady: 'all',
  date: '',
};

const emptyForm = () => ({
  recordId: '',
  batchId: '',
  packageType: 'ONE_KG_BAG',
  packageQuantityKg: '',
  lotCode: '',
  exportReady: false,
  packagingDate: todayIsoDate(),
  equipmentId: '',
  operatorId: '',
});

const PackagingRecordList = ({ onOpenCompliance }) => {
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
        packagingApi.getAllPackagingRecords(),
        batchApi.getAllBatches(),
        equipmentApi.getAllEquipment(),
        operatorApi.getOperatorsByRole('PACKAGER'),
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
    // Deferred so the initial setState happens outside the effect body (same
    // pattern as the Water Supply views).
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const equipmentName = useMemo(() => {
    const byId = new Map(equipment.map((item) => [item.equipmentId, item.equipmentName]));
    return (equipmentId) => byId.get(equipmentId) || equipmentId || '—';
  }, [equipment]);

  const operatorName = useMemo(() => {
    const byId = new Map(operators.map((operator) => [operator.operatorId, operator.operatorName]));
    return (operatorId) => byId.get(operatorId) || operatorId || '—';
  }, [operators]);

  const filteredRecords = useMemo(() => {
    return records
      .filter((record) => {
        if (filters.batchId !== 'all' && record.batchId !== filters.batchId) return false;
        if (filters.packageType !== 'all' && record.packageType !== filters.packageType) return false;
        if (filters.exportReady === 'yes' && !record.exportReady) return false;
        if (filters.exportReady === 'no' && record.exportReady) return false;
        if (filters.date && String(record.packagingDate || '').slice(0, 10) !== filters.date)
          return false;
        return true;
      })
      .sort((a, b) => String(b.packagingDate || '').localeCompare(String(a.packagingDate || '')));
  }, [records, filters]);

  const packageSummary = useMemo(() => {
    return PACKAGE_TYPES.map((type) => {
      const rows = records.filter((record) => record.packageType === type);
      const totalKg = rows.reduce((sum, record) => sum + Number(record.packageQuantityKg || 0), 0);
      return { type, count: rows.length, totalKg };
    });
  }, [records]);

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
      packageType: record.packageType || 'ONE_KG_BAG',
      packageQuantityKg: String(record.packageQuantityKg ?? ''),
      lotCode: record.lotCode || '',
      exportReady: Boolean(record.exportReady),
      packagingDate: record.packagingDate
        ? String(record.packagingDate).slice(0, 10)
        : todayIsoDate(),
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

  const { titleId, panelRef, handleBackdropClick } = useAccessibleDialog({
    open: showForm,
    onClose: closeForm,
    closeOnBackdrop: !saving,
    closeOnEscape: !saving,
  });

  const updateForm = (key) => (event) => {
    const value = key === 'exportReady' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [key]: value }));
  };

  const validate = () => {
    if (!editing && !form.recordId.trim()) return 'Record ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (!form.packageType) return 'Package type is required';
    if (form.packageQuantityKg === '' || Number(form.packageQuantityKg) <= 0) {
      return 'Package quantity must be greater than 0';
    }
    if (!form.lotCode.trim()) return 'Lot code is required';
    if (!form.packagingDate) return 'Packaging date is required';
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
        packageType: form.packageType,
        packageQuantityKg: Number(form.packageQuantityKg),
        exportReady: Boolean(form.exportReady),
        packagingDate: form.packagingDate,
        equipmentId: form.equipmentId || null,
        operatorId: form.operatorId || null,
      };

      if (editing) {
        await packagingApi.updatePackagingRecord(editing.recordId, payload);
      } else {
        await packagingApi.createPackagingRecord({
          recordId: form.recordId.trim(),
          batchId: form.batchId,
          lotCode: form.lotCode.trim(),
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the packaging record');
    } finally {
      setSaving(false);
    }
  };

  const handleMarkExportReady = async (record) => {
    setActionError('');
    try {
      await packagingApi.markExportReady(record.recordId);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to mark the record as export ready');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await packagingApi.deletePackagingRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the packaging record');
    } finally {
      setDeleting(false);
    }
  };

  const header = (
    <PTListHeader
      title="Packaging"
      subtitle="Track lot codes, packaged quantities and export readiness per batch."
    >
      <button type="button" onClick={openCreate} className={PRIMARY_BUTTON}>
        <Plus className="w-4 h-4" />
        New Packaging Record
      </button>
    </PTListHeader>
  );

  if (loading) return <LoadingState message="Loading packaging records..." />;
  if (error)
    return (
      <div className="p-6">
        {header}
        <ErrorState message={error?.message || 'Failed to load packaging records'} onRetry={load} />
      </div>
    );

  return (
    <div className="p-6">
      {header}

      {actionError && (
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {actionError}
        </div>
      )}

      {/* Quantity by package type */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        {packageSummary.map((summary) => (
          <div key={summary.type} className={CARD}>
            <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
              {packageLabel(summary.type)}
            </p>
            <p className={`${PAGE_TITLE} mt-1`}>
              {formatNumber(summary.totalKg, 2)} kg
            </p>
            <p className="text-sm text-gray-600">
              {summary.count} record{summary.count === 1 ? '' : 's'}
            </p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className={`${SECTION_CARD} mb-6`}>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex items-center gap-2 text-gray-500">
            <Filter className="w-5 h-5" />
            <span className="text-sm font-medium">Filters</span>
          </div>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Batch
            <select
              aria-label="Filter by batch"
              value={filters.batchId}
              onChange={updateFilter('batchId')}
              className={FILTER_INPUT}
            >
              <option value="all">All batches</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Package Type
            <select
              aria-label="Filter by package type"
              value={filters.packageType}
              onChange={updateFilter('packageType')}
              className={FILTER_INPUT}
            >
              <option value="all">All types</option>
              {PACKAGE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {packageLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Export
            <select
              aria-label="Filter by export readiness"
              value={filters.exportReady}
              onChange={updateFilter('exportReady')}
              className={FILTER_INPUT}
            >
              <option value="all">All records</option>
              <option value="yes">Export ready</option>
              <option value="no">Not export ready</option>
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Packaging Date
            <input
              type="date"
              aria-label="Filter by packaging date"
              value={filters.date}
              onChange={updateFilter('date')}
              className={FILTER_INPUT}
            />
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredRecords.length} records
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredRecords.length === 0 ? (
        <div className={CARD_FRAME}>
          <EmptyState
            message={records.length === 0 ? 'No packaging records found' : 'No records match the filters'}
            actionLabel={records.length === 0 ? 'New Packaging Record' : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                {[
                  'Record',
                  'Batch',
                  'Package Type',
                  'Quantity',
                  'Lot Code',
                  'Packaging Date',
                  'Export',
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
              {filteredRecords.map((record) => (
                <tr key={record.recordId} className="hover:bg-gray-50">
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{record.recordId}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {record.batchId}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {packageLabel(record.packageType)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatWeight(record.packageQuantityKg)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span
                      className="text-xs font-mono text-gray-700"
                      title={`${record.lotCode} · equipment ${equipmentName(
                        record.equipmentId,
                      )} · operator ${operatorName(record.operatorId)}`}
                    >
                      {record.lotCode}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatIsoDate(record.packagingDate)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    {record.exportReady ? (
                      <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                        Export ready
                      </span>
                    ) : (
                      <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-700">
                        Domestic
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="inline-flex items-center gap-1">
                      {onOpenCompliance && (
                        <button
                          type="button"
                          aria-label="Compliance"
                          title="Open compliance records for this batch"
                          onClick={() => onOpenCompliance(record.batchId)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-gray-600 hover:bg-gray-100 hover:text-gray-800"
                        >
                          <Link2 className="w-4 h-4" />
                        </button>
                      )}
                      {!record.exportReady && (
                        <button
                          type="button"
                          aria-label="Mark ready"
                          title="Mark as export ready"
                          onClick={() => handleMarkExportReady(record)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-green-700 hover:bg-green-50 hover:text-green-800"
                        >
                          <PackageCheck className="w-4 h-4" />
                        </button>
                      )}
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
          aria-labelledby={titleId}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={handleBackdropClick}
        >
          <div
            ref={panelRef}
            tabIndex={-1}
            className="bg-white rounded-2xl p-6 shadow-card w-full max-w-lg max-h-[90vh] overflow-y-auto outline-none"
          >
            <h2 id={titleId} className="font-heading text-xl font-bold text-on-background mb-4">
              {editing ? 'Edit Packaging Record' : 'New Packaging Record'}
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
                  Record ID
                  <input
                    type="text"
                    value={form.recordId}
                    onChange={updateForm('recordId')}
                    disabled={Boolean(editing)}
                    className={`${FIELD_INPUT} disabled:bg-gray-100`}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Batch
                  <select
                    value={form.batchId}
                    onChange={updateForm('batchId')}
                    disabled={Boolean(editing)}
                    className={`${FIELD_INPUT} disabled:bg-gray-100`}
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

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Package Type
                  <select
                    value={form.packageType}
                    onChange={updateForm('packageType')}
                    className={FIELD_INPUT}
                  >
                    {PACKAGE_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {packageLabel(type)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Quantity (kg)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.packageQuantityKg}
                    onChange={updateForm('packageQuantityKg')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="flex flex-col gap-1">
                <label
                  htmlFor="packaging-lot-code"
                  className="text-sm font-medium text-gray-700"
                >
                  Lot Code
                </label>
                <div className="flex gap-2">
                  <input
                    id="packaging-lot-code"
                    type="text"
                    value={form.lotCode}
                    onChange={updateForm('lotCode')}
                    disabled={Boolean(editing)}
                    className={`flex-1 ${FIELD_INPUT} disabled:bg-gray-100`}
                  />
                  {!editing && (
                    <button
                      type="button"
                      onClick={() =>
                        setForm((current) => ({
                          ...current,
                          lotCode: suggestLotCode(current.batchId, current.packagingDate),
                        }))
                      }
                      className={SECONDARY_BUTTON}
                      title="Suggest an editable lot code"
                    >
                      <Sparkles className="w-4 h-4" />
                      Suggest
                    </button>
                  )}
                </div>
                {!editing && (
                  <span className="text-xs font-normal text-gray-500">
                    Editable suggestion; the backend enforces uniqueness.
                  </span>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Packaging Date
                  <input
                    type="date"
                    value={form.packagingDate}
                    onChange={updateForm('packagingDate')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className="flex items-center gap-2 mt-6 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    checked={form.exportReady}
                    onChange={updateForm('exportReady')}
                  />
                  Export ready
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Equipment
                  <select
                    value={form.equipmentId}
                    onChange={updateForm('equipmentId')}
                    className={FIELD_INPUT}
                  >
                    <option value="">No equipment</option>
                    {equipment.map((item) => (
                      <option key={item.equipmentId} value={item.equipmentId}>
                        {item.equipmentName} ({item.equipmentId})
                      </option>
                    ))}
                  </select>
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Operator
                  <select
                    value={form.operatorId}
                    onChange={updateForm('operatorId')}
                    className={FIELD_INPUT}
                  >
                    <option value="">No operator</option>
                    {operators.map((operator) => (
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
          title="Delete packaging record"
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

export default PackagingRecordList;
