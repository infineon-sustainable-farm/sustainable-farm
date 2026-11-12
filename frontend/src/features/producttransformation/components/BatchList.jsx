/**
 * Product Transformation - Batch List Component
 * Displays all batches with filtering and status management
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useState } from 'react';
import { batchApi } from '../api/batchApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import { formatDate, formatWeight, getStatusColor } from '../utils/format';
import { Plus, Filter, ArrowRight, Package } from 'lucide-react';

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

  if (loading) return <LoadingState message="Loading batches..." />;
  if (error)
    return <ErrorState message={error?.message || 'Failed to load batches'} onRetry={load} />;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Batch Management</h1>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
        >
          <Plus className="w-4 h-4" />
          New Batch
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

      {/* Filter Bar */}
      <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
        <div className="flex items-center gap-4">
          <Filter className="w-5 h-5 text-gray-500" />
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary"
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
        <div className="bg-white rounded-lg shadow-sm p-12 text-center">
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">No batches found</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-sm overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Batch ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Harvest Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Variety
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Location
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredBatches.map((batch) => (
                <tr key={batch.batchId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{batch.batchId}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-600">{formatDate(batch.harvestDate)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{batch.mangoVariety}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{formatWeight(batch.harvestQuantityKg)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(batch.currentStatus)}`}>
                      {batch.currentStatus}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-600">
                      {batch.farmId} / {batch.blockId}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    {batch.currentStatus !== 'SHIPPED' && batch.currentStatus !== 'REJECTED' && (
                      <button
                        onClick={() => handleAdvanceStatus(batch.batchId)}
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
          aria-label="New batch"
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-lg p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-xl font-bold text-gray-900 mb-4">New Batch</h2>

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
                  Batch ID
                  <input
                    type="text"
                    value={form.batchId}
                    onChange={updateForm('batchId')}
                    placeholder="LOT-DEMO-006"
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Harvest date
                  <input
                    type="date"
                    value={form.harvestDate}
                    onChange={updateForm('harvestDate')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Mango variety
                  <select
                    value={form.mangoVariety}
                    onChange={updateForm('mangoVariety')}
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  >
                    {MANGO_VARIETIES.map((variety) => (
                      <option key={variety} value={variety}>
                        {variety}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Harvest quantity (kg)
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.harvestQuantityKg}
                    onChange={updateForm('harvestQuantityKg')}
                    placeholder="520.00"
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Farm ID
                  <input
                    type="text"
                    value={form.farmId}
                    onChange={updateForm('farmId')}
                    placeholder="FERME-DEMO"
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>

                <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
                  Block ID
                  <input
                    type="text"
                    value={form.blockId}
                    onChange={updateForm('blockId')}
                    placeholder="BLOC-A"
                    className="border border-gray-300 rounded-lg px-3 py-2"
                  />
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={saving}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-60"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 disabled:opacity-60"
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
