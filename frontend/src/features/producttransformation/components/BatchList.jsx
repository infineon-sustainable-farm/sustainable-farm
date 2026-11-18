/**
 * Product Transformation - Batch screen
 * Displays all batches with filtering and status management
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useState } from 'react';
import { Package, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, EmptyState, ErrorState, PageHeader, Select } from '../../../shared/ui';
import { batchApi } from '../api/batchApi';
import BatchTable from './BatchTable';
import FilterBar from './FilterBar';
import FormAlert from './FormAlert';
import { BATCH_STATUSES, batchStatusLabel } from './batchOptions';

const BatchList = () => {
  const { t } = useTranslation('producttransformation');
  const [filter, setFilter] = useState('all');
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState('');

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
      setActionError(t('batches.advanceError'));
    }
  };

  const filteredBatches = batches.filter((batch) => {
    if (filter === 'all') return true;
    return batch.currentStatus === filter;
  });

  const renderContent = () => {
    if (loading) return <BatchTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('batches.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        {actionError && <FormAlert className="mb-4">{actionError}</FormAlert>}

        <FilterBar showLabel={false} count={t('batches.count', { count: filteredBatches.length })}>
          <Select aria-label={t('batches.filterStatus')} value={filter} onChange={(e) => setFilter(e.target.value)} className="w-auto">
            <option value="all">{t('batches.allStatuses')}</option>
            {BATCH_STATUSES.map((status) => (
              <option key={status} value={status}>
                {batchStatusLabel(status)}
              </option>
            ))}
          </Select>
        </FilterBar>

        {filteredBatches.length === 0 ? (
          <EmptyState icon={Package} message={t('batches.empty')} />
        ) : (
          <BatchTable batches={filteredBatches} onAdvance={handleAdvanceStatus} />
        )}
      </>
    );
  };

  return (
    <div className="p-6">
      {/* "New Batch" has never been wired to a form; it is kept as it was (see the T30 report). */}
      <PageHeader className="mb-6" title={t('batches.title')} actions={<Button icon={Plus}>{t('batches.new')}</Button>} />
      {renderContent()}
    </div>
  );
};

export default BatchList;
