/**
 * Product Transformation - Batch List Component
 * Displays all batches with filtering and status management
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useState } from 'react';
import { useApi } from '../../../shared/hooks/useApi';
import { batchApi } from '../api/batchApi';
import LoadingState from '../../../shared/components/LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import { formatDate, formatWeight, getStatusColor } from '../../../shared/utils/formatUtils';
import { Plus, Filter, ArrowRight, Package } from 'lucide-react';

const BatchList = () => {
  const [filter, setFilter] = useState('all');
  const { data: batches, loading, error, refetch } = useApi('/api/batches');

  const handleAdvanceStatus = async (batchId) => {
    try {
      await batchApi.advanceBatchStatus(batchId);
      refetch();
    } catch (err) {
      console.error('Failed to advance status:', err);
      alert('Failed to advance batch status. Please check QC requirements.');
    }
  };

  const filteredBatches = batches?.filter((batch) => {
    if (filter === 'all') return true;
    return batch.currentStatus === filter;
  }) || [];

  if (loading) return <LoadingState message="Loading batches..." />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Batch Management</h1>
        <button className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors">
          <Plus className="w-4 h-4" />
          New Batch
        </button>
      </div>

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
    </div>
  );
};

export default BatchList;
