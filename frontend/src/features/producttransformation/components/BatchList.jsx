/**
 * Product Transformation - Batch List Component
 * Displays all batches with filtering and status management
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { batchApi } from '../api/batchApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import { useAccessibleDialog } from '../../../shared/hooks/useAccessibleDialog';
import { formatDate, formatWeight, getStatusColor } from '../utils/format';
import { Plus, Filter, ArrowRight, Package } from 'lucide-react';

import PTListHeader from './PTListHeader';
import { CARD_FRAME, FIELD_INPUT, FIELD_LABEL, FILTER_INPUT, PRIMARY_BUTTON, SECONDARY_BUTTON, SECTION_CARD, TABLE_CARD, TABLE_HEAD } from '../styles/ptStyles';

const MANGO_VARIETIES = ['KEITT', 'KENT', 'TOMMY', 'AMÉLIE', 'OTHER'];

const todayIsoDate = () => new Date().toISOString().slice(0, 10);

const emptyForm = () => ({
  batchId: '',
  harvestDate: todayIsoDate(),
  mangoVariety: 'KEITT',
  harvestQuantityKg: '',
  farmId: '',
  blockId: '',
});

const BatchList = () => {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('all');
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await batchApi.getAllBatches();
      setBatches(Array.isArray(data) ? data : []);
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

  const handleAdvanceStatus = async (batchId) => {
    setActionError('');
    try {
      await batchApi.advanceBatchStatus(batchId);
      await load();
    } catch (err) {
      console.error('Failed to advance status:', err);
      setActionError('Failed to advance batch status. Please check QC requirements.');
    }
  };

  const openCreate = () => {
    setForm(emptyForm());
    setFormError('');
    setActionError('');
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;
    setShowForm(false);
    setFormError('');
  };

  const { titleId, panelRef, handleBackdropClick } = useAccessibleDialog({
    open: showForm,
    onClose: closeForm,
    closeOnBackdrop: !saving,
    closeOnEscape: !saving,
  });

  const updateForm = (key) => (event) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  const validate = () => {
    if (!form.batchId.trim()) return 'Batch ID is required';
    if (!form.harvestDate) return 'Harvest date is required';
    if (!form.mangoVariety) return 'Mango variety is required';
    if (form.harvestQuantityKg === '' || Number(form.harvestQuantityKg) <= 0) {
      return 'Harvest quantity must be greater than 0';
    }
    if (!form.farmId.trim()) return 'Farm ID is required';
    if (!form.blockId.trim()) return 'Block ID is required';
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
      await batchApi.createBatch({
        batchId: form.batchId.trim(),
        harvestDate: form.harvestDate,
        mangoVariety: form.mangoVariety,
        harvestQuantityKg: Number(form.harvestQuantityKg),
        farmId: form.farmId.trim(),
        blockId: form.blockId.trim(),
      });
      setShowForm(false);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to create the batch');
    } finally {
      setSaving(false);
    }
  };

  const filteredBatches = batches.filter((batch) => {
    if (filter === 'all') return true;
    return batch.currentStatus === filter;
  });

  const header = (
    <PTListHeader title="Batch Management">
      <button type="button" onClick={openCreate} className={PRIMARY_BUTTON}>
        <Plus className="w-4 h-4" />
        New Batch
      </button>
    </PTListHeader>
  );

  if (loading) return <LoadingState message="Loading batches..." />;
  if (error)
    return (
      <div className="p-6">
        {header}
        <ErrorState message={error?.message || 'Failed to load batches'} onRetry={load} />
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

      {/* Filter Bar */}
      <div className={`${SECTION_CARD} mb-6`}>
        <div className="flex items-center gap-4">
          <Filter className="w-5 h-5 text-gray-500" />
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className={FILTER_INPUT}
          >
            <option value="all">All Statuses</option>
            <option value="CREATED">Created</option>
            <option value="INTAKE">Intake</option>
            <option value="WASHING">Washing</option>
            <option value="DRYING">Drying</option>
            <option value="PACKAGING">Packaging</option>
            <option value="COMPLETED">Completed</option>
            <option value="SHIPPED">Shipped</option>
            <option value="REJECTED">Rejected</option>
          </select>
          <span className="text-gray-600">{filteredBatches.length} batches</span>
        </div>
      </div>

      {/* Batches Table */}
      {filteredBatches.length === 0 ? (
        <div className={`${CARD_FRAME} p-12 text-center`}>
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">No batches found</p>
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                <th className={TABLE_HEAD}>
                  Batch ID
                </th>
                <th className={TABLE_HEAD}>
                  Harvest Date
                </th>
                <th className={TABLE_HEAD}>
                  Variety
                </th>
                <th className={TABLE_HEAD}>
                  Quantity
                </th>
                <th className={TABLE_HEAD}>
                  Status
                </th>
                <th className={TABLE_HEAD}>
                  Location
                </th>
                <th className={`${TABLE_HEAD} text-right`}>
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredBatches.map((batch) => (
                <tr
                  key={batch.batchId}
                  onClick={() =>
                    navigate(`/product-transformation/batches/${batch.batchId}`)
                  }
                  className="cursor-pointer hover:bg-gray-50"
                  title="Open traceability timeline"
                >
                  <td className="px-4 py-4 whitespace-nowrap">
                    <Link
                      to={`/product-transformation/batches/${batch.batchId}`}
                      onClick={(event) => event.stopPropagation()}
                      className="text-sm font-medium text-primary hover:underline"
                    >
                      {batch.batchId}
                    </Link>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-600">{formatDate(batch.harvestDate)}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{batch.mangoVariety}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{formatWeight(batch.harvestQuantityKg)}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(batch.currentStatus)}`}>
                      {batch.currentStatus}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-600">
                      {batch.farmId} / {batch.blockId}
                    </div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {batch.currentStatus !== 'SHIPPED' && batch.currentStatus !== 'REJECTED' && (
                      <button
                        onClick={(event) => {
                          // The row itself opens the timeline: keep Advance local.
                          event.stopPropagation();
                          handleAdvanceStatus(batch.batchId);
                        }}
                        className="text-primary hover:text-primary/80 flex items-center gap-1 ml-auto"
                      >
                        Advance <ArrowRight className="w-4 h-4" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
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
            <h2 id={titleId} className="font-heading text-xl font-bold text-on-background mb-4">New Batch</h2>

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
                  Batch ID
                  <input
                    type="text"
                    value={form.batchId}
                    onChange={updateForm('batchId')}
                    placeholder="LOT-DEMO-006"
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Harvest date
                  <input
                    type="date"
                    value={form.harvestDate}
                    onChange={updateForm('harvestDate')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Mango variety
                  <select
                    value={form.mangoVariety}
                    onChange={updateForm('mangoVariety')}
                    className={FIELD_INPUT}
                  >
                    {MANGO_VARIETIES.map((variety) => (
                      <option key={variety} value={variety}>
                        {variety}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Harvest quantity (kg)
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.harvestQuantityKg}
                    onChange={updateForm('harvestQuantityKg')}
                    placeholder="520.00"
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Farm ID
                  <input
                    type="text"
                    value={form.farmId}
                    onChange={updateForm('farmId')}
                    placeholder="FERME-DEMO"
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Block ID
                  <input
                    type="text"
                    value={form.blockId}
                    onChange={updateForm('blockId')}
                    placeholder="BLOC-A"
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
                  {saving ? 'Saving...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BatchList;
