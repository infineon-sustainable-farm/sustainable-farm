/**
 * Product Transformation - Raw Intake List Component
 * Displays, filters, creates, edits and deletes raw intake (reception) records.
 *
 * Data contract: /api/raw-intakes (see RawIntakeController).
 * The backend enforces positive received quantities; IDs and batch links are
 * immutable after creation.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { batchApi } from '../api/batchApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { formatWeight } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import { Plus, Filter, Pencil, Trash2 } from 'lucide-react';

const MANGO_VARIETIES = ['KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER'];
const QUALITY_GRADES = ['A', 'B', 'C', 'D'];

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

const EMPTY_FILTERS = {
  batchId: 'all',
  grade: 'all',
  variety: 'all',
  date: '',
};

const emptyForm = () => ({
  intakeId: '',
  batchId: '',
  sourceFarm: '',
  sourceBlock: '',
  intakeDate: todayIsoDate(),
  receivedQuantityKg: '',
  receivedVariety: 'KEITT',
  receivedGrade: 'A',
  intakeOperator: '',
});

const RawIntakeList = () => {
  const [intakes, setIntakes] = useState([]);
  const [batches, setBatches] = useState([]);
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
      const [intakeData, batchData] = await Promise.all([
        rawIntakeApi.getAllRawIntakes(),
        batchApi.getAllBatches(),
      ]);
      setIntakes(Array.isArray(intakeData) ? intakeData : []);
      setBatches(Array.isArray(batchData) ? batchData : []);
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

  const filteredIntakes = useMemo(() => {
    return intakes
      .filter((intake) => {
        if (filters.batchId !== 'all' && intake.batchId !== filters.batchId) return false;
        if (filters.grade !== 'all' && intake.receivedGrade !== filters.grade) return false;
        if (filters.variety !== 'all' && intake.receivedVariety !== filters.variety) return false;
        if (filters.date && String(intake.intakeDate || '').slice(0, 10) !== filters.date)
          return false;
        return true;
      })
      .sort((a, b) => String(b.intakeDate || '').localeCompare(String(a.intakeDate || '')));
  }, [intakes, filters]);

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

  const openEdit = (intake) => {
    setEditing(intake);
    setForm({
      intakeId: intake.intakeId || '',
      batchId: intake.batchId || '',
      sourceFarm: intake.sourceFarm || '',
      sourceBlock: intake.sourceBlock || '',
      intakeDate: intake.intakeDate ? String(intake.intakeDate).slice(0, 10) : todayIsoDate(),
      receivedQuantityKg: String(intake.receivedQuantityKg ?? ''),
      receivedVariety: intake.receivedVariety || 'KEITT',
      receivedGrade: intake.receivedGrade || 'A',
      intakeOperator: intake.intakeOperator || '',
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
    if (!editing && !form.intakeId.trim()) return 'Intake ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (!form.intakeDate) return 'Intake date is required';
    if (!form.sourceFarm.trim()) return 'Source farm is required';
    if (!form.sourceBlock.trim()) return 'Source block is required';
    if (form.receivedQuantityKg === '' || Number(form.receivedQuantityKg) <= 0) {
      return 'Received quantity must be greater than 0';
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
        sourceFarm: form.sourceFarm.trim(),
        sourceBlock: form.sourceBlock.trim(),
        intakeDate: form.intakeDate,
        receivedQuantityKg: Number(form.receivedQuantityKg),
        receivedVariety: form.receivedVariety,
        receivedGrade: form.receivedGrade,
        intakeOperator: form.intakeOperator.trim() || null,
      };

      if (editing) {
        await rawIntakeApi.updateRawIntake(editing.intakeId, payload);
      } else {
        await rawIntakeApi.createRawIntake({
          intakeId: form.intakeId.trim(),
          batchId: form.batchId,
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the raw intake');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await rawIntakeApi.deleteRawIntake(deleteTarget.intakeId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the raw intake');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <LoadingState message="Loading raw intakes..." />;
  if (error)
    return <ErrorState message={error?.message || 'Failed to load raw intakes'} onRetry={load} />;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Raw Intake</h1>
          <p className="text-sm text-gray-600 mt-1">
            Record mango receptions, their source and the quantity entering the line.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
        >
          <Plus className="w-4 h-4" />
          New Raw Intake
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
            Variety
            <select
              aria-label="Filter by variety"
              value={filters.variety}
              onChange={updateFilter('variety')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All varieties</option>
              {MANGO_VARIETIES.map((variety) => (
                <option key={variety} value={variety}>
                  {formatEnumLabel(variety)}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Grade
            <select
              aria-label="Filter by grade"
              value={filters.grade}
              onChange={updateFilter('grade')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">All grades</option>
              {QUALITY_GRADES.map((grade) => (
                <option key={grade} value={grade}>
                  Grade {grade}
                </option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-gray-600">
            Intake Date
            <input
              type="date"
              aria-label="Filter by intake date"
              value={filters.date}
              onChange={updateFilter('date')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredIntakes.length} records
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredIntakes.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm">
          <EmptyState
            message={intakes.length === 0 ? 'No raw intakes found' : 'No records match the filters'}
            actionLabel={intakes.length === 0 ? 'New Raw Intake' : undefined}
            onAction={intakes.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                {[
                  'Intake',
                  'Batch',
                  'Date',
                  'Source',
                  'Quantity',
                  'Variety',
                  'Grade',
                  'Operator',
                  'Actions',
                ].map((label) => (
                  <th
                    key={label}
                    className={`px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider ${
                      label === 'Actions' ? 'text-right' : 'text-left'
                    }`}
                  >
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredIntakes.map((intake) => (
                <tr key={intake.intakeId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{intake.intakeId}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {intake.batchId}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatIsoDate(intake.intakeDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {intake.sourceFarm} / {intake.sourceBlock}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatWeight(intake.receivedQuantityKg)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {intake.receivedVariety ? formatEnumLabel(intake.receivedVariety) : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-700">
                      Grade {intake.receivedGrade || 'N/A'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {intake.intakeOperator || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="inline-flex items-center gap-1">
                      <button
                        type="button"
                        aria-label="Edit"
                        title="Edit"
                        onClick={() => openEdit(intake)}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-md text-primary hover:bg-primary/10 hover:text-primary/80"
                      >
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        aria-label="Delete"
                        title="Delete"
                        onClick={() => setDeleteTarget(intake)}
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
          aria-label={editing ? 'Edit raw intake' : 'New raw intake'}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-lg p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              {editing ? 'Edit Raw Intake' : 'New Raw Intake'}
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
                  Intake ID
                  <input
                    type="text"
                    value={form.intakeId}
                    onChange={updateForm('intakeId')}
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

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Source Farm
                  <input
                    type="text"
                    value={form.sourceFarm}
                    onChange={updateForm('sourceFarm')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Source Block
                  <input
                    type="text"
                    value={form.sourceBlock}
                    onChange={updateForm('sourceBlock')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Intake Date
                  <input
                    type="date"
                    value={form.intakeDate}
                    onChange={updateForm('intakeDate')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Quantity (kg)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.receivedQuantityKg}
                    onChange={updateForm('receivedQuantityKg')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Variety
                  <select
                    value={form.receivedVariety}
                    onChange={updateForm('receivedVariety')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    {MANGO_VARIETIES.map((variety) => (
                      <option key={variety} value={variety}>
                        {formatEnumLabel(variety)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Grade
                  <select
                    value={form.receivedGrade}
                    onChange={updateForm('receivedGrade')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    {QUALITY_GRADES.map((grade) => (
                      <option key={grade} value={grade}>
                        Grade {grade}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                Intake Operator
                <input
                  type="text"
                  value={form.intakeOperator}
                  onChange={updateForm('intakeOperator')}
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
          title="Delete raw intake"
          message={`Delete intake ${deleteTarget.intakeId}? This action cannot be undone.`}
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

export default RawIntakeList;
