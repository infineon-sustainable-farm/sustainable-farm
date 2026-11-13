/**
 * Product Transformation - Drying Run List Component
 * Displays, filters, creates, edits and deletes drying runs.
 *
 * Data contract: /api/drying-runs (see DryingRunController).
 * The backend enforces a 6–18 % end-moisture range for EU compliance, plus
 * active drying equipment and operators with the DRYER role.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { dryingApi } from '../api/dryingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useAccessibleDialog } from '../../../shared/hooks/useAccessibleDialog';
import { formatNumber, formatDateTime } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { Plus, Filter, Pencil, Trash2 } from 'lucide-react';

import PTListHeader from './PTListHeader';
import { CARD_FRAME, FIELD_INPUT, FIELD_LABEL, FILTER_INPUT, PRIMARY_BUTTON, SECONDARY_BUTTON, SECTION_CARD, TABLE_CARD, TABLE_HEAD_BASE } from '../styles/ptStyles';

const DRYER_TYPES = ['DRYING', 'SOLAR_DRYER', 'TUNNEL_DRYER', 'TESTA_DRYER'];
const MIN_END_MOISTURE = 6;
const MAX_END_MOISTURE = 18;

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
  runId: '',
  batchId: '',
  durationHours: '',
  targetTemperatureC: '',
  actualTemperatureC: '',
  startMoisturePct: '',
  endMoisturePct: '',
  energyUsageKwh: '',
  startTime: '',
  endTime: '',
  equipmentId: '',
  operatorId: '',
});

const DryingRunList = () => {
  const [runs, setRuns] = useState([]);
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
      const [runData, batchData, equipmentData, operatorData] = await Promise.all([
        dryingApi.getAllDryingRuns(),
        batchApi.getAllBatches(),
        equipmentApi.getAllEquipment(),
        operatorApi.getAllOperators(),
      ]);
      setRuns(Array.isArray(runData) ? runData : []);
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

  const dryers = useMemo(
    () => equipment.filter((item) => DRYER_TYPES.includes(item.equipmentType)),
    [equipment],
  );

  // Every operator is used to resolve names on existing records; only DRYERs
  // can be assigned to a new run (the backend enforces that rule).
  const dryerOperators = useMemo(
    () => operators.filter((operator) => operator.role === 'DRYER'),
    [operators],
  );

  const filteredRuns = useMemo(() => {
    return runs
      .filter((run) => {
        if (filters.batchId !== 'all' && run.batchId !== filters.batchId) return false;
        if (filters.equipmentId !== 'all' && run.equipmentId !== filters.equipmentId) return false;
        if (filters.date && String(run.startTime || '').slice(0, 10) !== filters.date) return false;
        return true;
      })
      .sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || '')));
  }, [runs, filters]);

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

  const openEdit = (run) => {
    setEditing(run);
    setForm({
      runId: run.runId || '',
      batchId: run.batchId || '',
      durationHours: String(run.durationHours ?? ''),
      targetTemperatureC: String(run.targetTemperatureC ?? ''),
      actualTemperatureC: String(run.actualTemperatureC ?? ''),
      startMoisturePct: String(run.startMoisturePct ?? ''),
      endMoisturePct: String(run.endMoisturePct ?? ''),
      energyUsageKwh: String(run.energyUsageKwh ?? ''),
      startTime: toDateTimeLocal(run.startTime),
      endTime: toDateTimeLocal(run.endTime),
      equipmentId: run.equipmentId || '',
      operatorId: run.operatorId || '',
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
    setForm((current) => ({ ...current, [key]: event.target.value }));
  };

  const validate = () => {
    if (!editing && !form.runId.trim()) return 'Run ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (form.durationHours === '' || Number(form.durationHours) <= 0) {
      return 'Duration must be greater than 0';
    }
    if (form.targetTemperatureC === '' || Number.isNaN(Number(form.targetTemperatureC))) {
      return 'Target temperature is required';
    }
    if (form.actualTemperatureC === '' || Number.isNaN(Number(form.actualTemperatureC))) {
      return 'Actual temperature is required';
    }
    if (form.startMoisturePct === '' || Number(form.startMoisturePct) <= 0) {
      return 'Start moisture must be greater than 0';
    }
    if (form.endMoisturePct === '') return 'End moisture is required';
    const endMoisture = Number(form.endMoisturePct);
    if (endMoisture < MIN_END_MOISTURE || endMoisture > MAX_END_MOISTURE) {
      return `End moisture must be between ${MIN_END_MOISTURE}% and ${MAX_END_MOISTURE}%`;
    }
    if (form.energyUsageKwh === '' || Number(form.energyUsageKwh) < 0) {
      return 'Energy usage cannot be negative';
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
        durationHours: Number(form.durationHours),
        targetTemperatureC: Number(form.targetTemperatureC),
        actualTemperatureC: Number(form.actualTemperatureC),
        startMoisturePct: Number(form.startMoisturePct),
        endMoisturePct: Number(form.endMoisturePct),
        energyUsageKwh: Number(form.energyUsageKwh),
        startTime: fromDateTimeLocal(form.startTime),
        endTime: fromDateTimeLocal(form.endTime),
        equipmentId: form.equipmentId || null,
        operatorId: form.operatorId || null,
      };

      if (editing) {
        await dryingApi.updateDryingRun(editing.runId, payload);
      } else {
        await dryingApi.createDryingRun({
          runId: form.runId.trim(),
          batchId: form.batchId,
          ...payload,
        });
      }

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the drying run');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await dryingApi.deleteDryingRun(deleteTarget.runId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the drying run');
    } finally {
      setDeleting(false);
    }
  };

  const header = (
    <PTListHeader
      title="Drying"
      subtitle="Track drying durations, temperatures, moisture loss and energy usage."
    >
      <button type="button" onClick={openCreate} className={PRIMARY_BUTTON}>
        <Plus className="w-4 h-4" />
        New Drying Run
      </button>
    </PTListHeader>
  );

  if (loading) return <LoadingState message="Loading drying runs..." />;
  if (error)
    return (
      <div className="p-6">
        {header}
        <ErrorState message={error?.message || 'Failed to load drying runs'} onRetry={load} />
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
            Equipment
            <select
              aria-label="Filter by equipment"
              value={filters.equipmentId}
              onChange={updateFilter('equipmentId')}
              className={FILTER_INPUT}
            >
              <option value="all">All equipment</option>
              {dryers.map((item) => (
                <option key={item.equipmentId} value={item.equipmentId}>
                  {item.equipmentName}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Start Date
            <input
              type="date"
              aria-label="Filter by start date"
              value={filters.date}
              onChange={updateFilter('date')}
              className={FILTER_INPUT}
            />
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredRuns.length} runs
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredRuns.length === 0 ? (
        <div className={CARD_FRAME}>
          <EmptyState
            message={runs.length === 0 ? 'No drying runs found' : 'No runs match the filters'}
            actionLabel={runs.length === 0 ? 'New Drying Run' : undefined}
            onAction={runs.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                {[
                  'Run',
                  'Batch',
                  'Duration',
                  'Temperature',
                  'Moisture',
                  'Energy',
                  'Period',
                  'Equipment / Operator',
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
              {filteredRuns.map((run) => {
                const equipmentLabel =
                  equipment.find((item) => item.equipmentId === run.equipmentId)?.equipmentName ||
                  run.equipmentId ||
                  'N/A';
                const operatorLabel =
                  operators.find((item) => item.operatorId === run.operatorId)?.operatorName ||
                  run.operatorId ||
                  'N/A';

                return (
                  <tr key={run.runId} className="hover:bg-gray-50">
                    <td className="px-3 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{run.runId}</div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {run.batchId}
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatNumber(run.durationHours, 2)} h
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatNumber(run.actualTemperatureC, 1)} °C
                      <span className="text-gray-500">
                        {' '}
                        / {formatNumber(run.targetTemperatureC, 1)} °C
                      </span>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatNumber(run.startMoisturePct, 1)}% → {formatNumber(run.endMoisturePct, 1)}%
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatNumber(run.energyUsageKwh, 2)} kWh
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-sm text-gray-600">
                      <div>{formatDateTime(run.startTime)}</div>
                      <div className="text-xs text-gray-500">to {formatDateTime(run.endTime)}</div>
                    </td>
                    <td className="px-3 py-4 text-sm text-gray-600">
                      <div className="text-gray-900">{equipmentLabel}</div>
                      <div className="text-xs text-gray-500">{operatorLabel}</div>
                    </td>
                    <td className="px-3 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="inline-flex items-center gap-1">
                        <button
                          type="button"
                          aria-label="Edit"
                          title="Edit"
                          onClick={() => openEdit(run)}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-md text-primary hover:bg-primary/10 hover:text-primary/80"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          aria-label="Delete"
                          title="Delete"
                          onClick={() => setDeleteTarget(run)}
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
              {editing ? 'Edit Drying Run' : 'New Drying Run'}
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
                  Run ID
                  <input
                    type="text"
                    value={form.runId}
                    onChange={updateForm('runId')}
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

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Duration (h)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.durationHours}
                    onChange={updateForm('durationHours')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Target Temp (°C)
                  <input
                    type="number"
                    step="0.1"
                    value={form.targetTemperatureC}
                    onChange={updateForm('targetTemperatureC')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Actual Temp (°C)
                  <input
                    type="number"
                    step="0.1"
                    value={form.actualTemperatureC}
                    onChange={updateForm('actualTemperatureC')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Start Moisture (%)
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.startMoisturePct}
                    onChange={updateForm('startMoisturePct')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  End Moisture (%)
                  <input
                    type="number"
                    min={MIN_END_MOISTURE}
                    max={MAX_END_MOISTURE}
                    step="0.01"
                    value={form.endMoisturePct}
                    onChange={updateForm('endMoisturePct')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Energy (kWh)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.energyUsageKwh}
                    onChange={updateForm('energyUsageKwh')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Start Time
                  <input
                    type="datetime-local"
                    value={form.startTime}
                    onChange={updateForm('startTime')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  End Time
                  <input
                    type="datetime-local"
                    value={form.endTime}
                    onChange={updateForm('endTime')}
                    className={FIELD_INPUT}
                  />
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
                    {dryers.map((item) => (
                      <option key={item.equipmentId} value={item.equipmentId}>
                        {item.equipmentName} · {formatEnumLabel(item.maintenanceStatus || 'ACTIVE')}
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
                    {dryerOperators.map((operator) => (
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
          title="Delete drying run"
          message={`Delete run ${deleteTarget.runId}? This action cannot be undone.`}
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

export default DryingRunList;
