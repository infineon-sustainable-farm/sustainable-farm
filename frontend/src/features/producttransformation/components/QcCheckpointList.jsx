/**
 * Product Transformation - QC Checkpoint List Component
 * Displays, filters, creates, edits and deletes quality control checkpoints.
 *
 * Data contract: /api/qc-checkpoints (see QcCheckpointController).
 * Business rule enforced by the backend: the inspector must have the
 * QC_INSPECTOR role, which is why the selector only lists those operators.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { qcApi } from '../api/qcApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { formatDateTime, getQcResultColor } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { Plus, Filter, Pencil, Trash2 } from 'lucide-react';

const QC_STAGES = ['INTAKE', 'WASHING', 'DRYING', 'COOLING', 'PACKAGING', 'FINAL'];
const QC_RESULTS = ['PENDING', 'PASS', 'FAIL', 'REWORK'];

const stageLabel = (stage) => (stage ? formatEnumLabel(stage) : '—');

const EMPTY_FILTERS = {
  batchId: 'all',
  stage: 'all',
  result: 'all',
  inspectorId: 'all',
  date: '',
};

const emptyForm = () => ({
  checkpointId: '',
  batchId: '',
  stage: 'INTAKE',
  result: 'PENDING',
  defectsCount: '0',
  inspectorId: '',
  checkpointTime: toDateTimeInput(new Date().toISOString()),
  defects: '',
  notes: '',
});

/**
 * Convert an ISO/QcCheckpoint timestamp into a `datetime-local` input value.
 * Backend returns LocalDateTime as `2026-11-09T10:30:00`.
 */
function toDateTimeInput(value) {
  if (!value) return '';
  return String(value).slice(0, 16);
}

/** Compare only the calendar day of two timestamp values. */
function matchesDate(value, isoDate) {
  if (!isoDate) return true;
  if (!value) return false;
  const day = new Date(value);
  if (Number.isNaN(day.getTime())) return false;
  const local = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(
    day.getDate(),
  ).padStart(2, '0')}`;
  return local === isoDate;
}

const QcCheckpointList = () => {
  const [checkpoints, setCheckpoints] = useState([]);
  const [batches, setBatches] = useState([]);
  const [inspectors, setInspectors] = useState([]);
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

  const loadCheckpoints = useCallback(async () => {
    return qcApi.getAllQcCheckpoints();
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [checkpointData, batchData, inspectorData] = await Promise.all([
        loadCheckpoints(),
        batchApi.getAllBatches(),
        operatorApi.getOperatorsByRole('QC_INSPECTOR'),
      ]);
      setCheckpoints(Array.isArray(checkpointData) ? checkpointData : []);
      setBatches(Array.isArray(batchData) ? batchData : []);
      setInspectors(Array.isArray(inspectorData) ? inspectorData : []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [loadCheckpoints]);

  useEffect(() => {
    // Deferred so the initial setState happens outside the effect body (same
    // pattern as the Water Supply views).
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const inspectorName = useMemo(() => {
    const byId = new Map(inspectors.map((operator) => [operator.operatorId, operator.operatorName]));
    return (inspectorId) => byId.get(inspectorId) || inspectorId || '—';
  }, [inspectors]);

  const filteredCheckpoints = useMemo(() => {
    return checkpoints
      .filter((checkpoint) => {
        if (filters.batchId !== 'all' && checkpoint.batchId !== filters.batchId) return false;
        if (filters.stage !== 'all' && checkpoint.stage !== filters.stage) return false;
        if (filters.result !== 'all' && checkpoint.result !== filters.result) return false;
        if (filters.inspectorId !== 'all' && checkpoint.inspectorId !== filters.inspectorId) return false;
        if (!matchesDate(checkpoint.checkpointTime, filters.date)) return false;
        return true;
      })
      .sort((a, b) => new Date(b.checkpointTime || 0) - new Date(a.checkpointTime || 0));
  }, [checkpoints, filters]);

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

  const openEdit = (checkpoint) => {
    setEditing(checkpoint);
    setForm({
      checkpointId: checkpoint.checkpointId || '',
      batchId: checkpoint.batchId || '',
      stage: checkpoint.stage || 'INTAKE',
      result: checkpoint.result || 'PENDING',
      defectsCount: String(checkpoint.defectsCount ?? 0),
      inspectorId: checkpoint.inspectorId || '',
      checkpointTime: toDateTimeInput(checkpoint.checkpointTime) || emptyForm().checkpointTime,
      defects: checkpoint.defects || '',
      notes: checkpoint.notes || '',
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
    if (!editing && !form.checkpointId.trim()) return 'Checkpoint ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (!form.stage) return 'QC stage is required';
    if (!form.result) return 'QC result is required';
    if (form.defectsCount === '' || Number(form.defectsCount) < 0) {
      return 'Defects count cannot be negative';
    }
    if (!form.checkpointTime) return 'Checkpoint time is required';
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
        stage: form.stage,
        result: form.result,
        defects: form.defects.trim() || null,
        defectsCount: Number(form.defectsCount),
        inspectorId: form.inspectorId || null,
        checkpointTime: form.checkpointTime,
        notes: form.notes.trim() || null,
      };

      if (editing) {
        await qcApi.updateQcCheckpoint(editing.checkpointId, payload);
      } else {
        await qcApi.createQcCheckpoint({
          checkpointId: form.checkpointId.trim(),
          batchId: form.batchId,
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the QC checkpoint');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await qcApi.deleteQcCheckpoint(deleteTarget.checkpointId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the QC checkpoint');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <LoadingState message="Loading QC checkpoints..." />;
  if (error) return <ErrorState message={error?.message || 'Failed to load QC checkpoints'} onRetry={load} />;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quality Control</h1>
          <p className="text-sm text-gray-600 mt-1">
            Record and track QC checkpoints for each batch and stage.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
        >
          <Plus className="w-4 h-4" />
          New QC Checkpoint
        </button>
      </div>

      {actionError && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
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
            Stage
            <select
              aria-label="Filter by stage"
              value={filters.stage}
              onChange={updateFilter('stage')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All stages</option>
              {QC_STAGES.map((stage) => (
                <option key={stage} value={stage}>
                  {formatEnumLabel(stage)}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Result
            <select
              aria-label="Filter by result"
              value={filters.result}
              onChange={updateFilter('result')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All results</option>
              {QC_RESULTS.map((result) => (
                <option key={result} value={result}>
                  {formatEnumLabel(result)}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Inspector
            <select
              aria-label="Filter by inspector"
              value={filters.inspectorId}
              onChange={updateFilter('inspectorId')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All inspectors</option>
              {inspectors.map((operator) => (
                <option key={operator.operatorId} value={operator.operatorId}>
                  {operator.operatorName}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Date
            <input
              type="date"
              aria-label="Filter by date"
              value={filters.date}
              onChange={updateFilter('date')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </label>

          <span className="text-gray-600 text-sm ml-auto">{filteredCheckpoints.length} checkpoints</span>
        </div>
      </div>

      {/* Table */}
      {filteredCheckpoints.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm">
          <EmptyState
            message={checkpoints.length === 0 ? 'No QC checkpoints found' : 'No checkpoints match the filters'}
            actionLabel={checkpoints.length === 0 ? 'New QC Checkpoint' : undefined}
            onAction={checkpoints.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                {['Checkpoint', 'Batch', 'Stage', 'Result', 'Defects', 'Inspector', 'Checkpoint Time', 'Actions'].map(
                  (label) => (
                    <th
                      key={label}
                      className={`px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider ${
                        label === 'Actions' ? 'text-right' : 'text-left'
                      }`}
                    >
                      {label}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredCheckpoints.map((checkpoint) => (
                <tr key={checkpoint.checkpointId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{checkpoint.checkpointId}</div>
                    {checkpoint.mandatory && (
                      <span className="text-xs text-amber-700">Mandatory</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{checkpoint.batchId}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {stageLabel(checkpoint.stage)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getQcResultColor(
                        checkpoint.result,
                      )}`}
                    >
                      {stageLabel(checkpoint.result)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {checkpoint.defectsCount ?? 0}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {inspectorName(checkpoint.inspectorId)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatDateTime(checkpoint.checkpointTime)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      type="button"
                      onClick={() => openEdit(checkpoint)}
                      className="text-primary hover:text-primary/80 inline-flex items-center gap-1 mr-3"
                    >
                      <Pencil className="w-4 h-4" />
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(checkpoint)}
                      className="text-red-600 hover:text-red-700 inline-flex items-center gap-1"
                    >
                      <Trash2 className="w-4 h-4" />
                      Delete
                    </button>
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
          aria-label={editing ? 'Edit QC checkpoint' : 'New QC checkpoint'}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-lg p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              {editing ? 'Edit QC Checkpoint' : 'New QC Checkpoint'}
            </h2>

            {formError && (
              <div role="alert" className="mb-4 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                Checkpoint ID
                <input
                  type="text"
                  value={form.checkpointId}
                  onChange={updateForm('checkpointId')}
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

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Stage
                  <select
                    value={form.stage}
                    onChange={updateForm('stage')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    {QC_STAGES.map((stage) => (
                      <option key={stage} value={stage}>
                        {formatEnumLabel(stage)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Result
                  <select
                    value={form.result}
                    onChange={updateForm('result')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    {QC_RESULTS.map((result) => (
                      <option key={result} value={result}>
                        {formatEnumLabel(result)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Defects Count
                  <input
                    type="number"
                    min="0"
                    value={form.defectsCount}
                    onChange={updateForm('defectsCount')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Checkpoint Time
                  <input
                    type="datetime-local"
                    value={form.checkpointTime}
                    onChange={updateForm('checkpointTime')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                Inspector
                <select
                  value={form.inspectorId}
                  onChange={updateForm('inspectorId')}
                  className="border border-gray-300 rounded-lg px-3 py-2"
                >
                  <option value="">No inspector</option>
                  {inspectors.map((operator) => (
                    <option key={operator.operatorId} value={operator.operatorId}>
                      {operator.operatorName}
                    </option>
                  ))}
                </select>
              </label>

              <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                Defects
                <textarea
                  value={form.defects}
                  onChange={updateForm('defects')}
                  rows={2}
                  className="border border-gray-300 rounded-lg px-3 py-2"
                />
              </label>

              <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                Notes
                <textarea
                  value={form.notes}
                  onChange={updateForm('notes')}
                  rows={2}
                  className="border border-gray-300 rounded-lg px-3 py-2"
                />
              </label>

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
          title="Delete QC checkpoint"
          message={`Delete checkpoint ${deleteTarget.checkpointId}? This action cannot be undone.`}
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

export default QcCheckpointList;
