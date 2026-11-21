/**
 * Product Transformation - Quality Control screen
 * Displays, filters, creates, edits and deletes quality control checkpoints.
 *
 * Data contract: /api/product-transformation/qc-checkpoints (see QcCheckpointController).
 * Business rule enforced by the backend: the inspector must have the
 * QC_INSPECTOR role, which is why the selector only lists those operators.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { qcApi } from '../api/qcApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';
import QcCheckpointFilters from './QcCheckpointFilters';
import QcCheckpointFormModal from './QcCheckpointFormModal';
import QcCheckpointTable from './QcCheckpointTable';
import { matchesDate, qcNone } from './qcOptions';

const EMPTY_FILTERS = {
  batchId: 'all',
  stage: 'all',
  result: 'all',
  inspectorId: 'all',
  date: '',
};

const QcCheckpointList = () => {
  const { t } = useTranslation('producttransformation');
  const [checkpoints, setCheckpoints] = useState([]);
  const [batches, setBatches] = useState([]);
  const [inspectors, setInspectors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [checkpointData, batchData, inspectorData] = await Promise.all([
        qcApi.getAllQcCheckpoints(),
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
  }, []);

  useEffect(() => {
    // Deferred so the initial setState happens outside the effect body.
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const inspectorName = useMemo(() => {
    const byId = new Map(inspectors.map((operator) => [operator.operatorId, operator.operatorName]));
    return (inspectorId) => byId.get(inspectorId) || inspectorId || qcNone();
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

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (checkpoint) => {
    setEditing(checkpoint);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
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

    closeForm();
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await qcApi.deleteQcCheckpoint(deleteTarget.checkpointId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('qc.deleteError'));
    } finally {
      setDeleting(false);
    }
  };

  const closeDelete = () => {
    if (deleting) return;
    setDeleteTarget(null);
    setDeleteError('');
  };

  const renderContent = () => {
    if (loading) return <QcCheckpointTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('qc.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        <QcCheckpointFilters
          filters={filters}
          onChange={updateFilter}
          batches={batches}
          inspectors={inspectors}
          count={filteredCheckpoints.length}
        />
        {filteredCheckpoints.length === 0 ? (
          <EmptyState
            message={checkpoints.length === 0 ? t('qc.empty') : t('qc.noMatch')}
            actionLabel={checkpoints.length === 0 ? t('qc.new') : undefined}
            onAction={checkpoints.length === 0 ? openCreate : undefined}
          />
        ) : (
          <QcCheckpointTable
            checkpoints={filteredCheckpoints}
            inspectorName={inspectorName}
            onEdit={openEdit}
            onDelete={setDeleteTarget}
          />
        )}
      </>
    );
  };

  return (
    <div className="p-6">
      <PageHeader
        className="mb-6"
        title={t('qc.title')}
        subtitle={t('qc.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('qc.new')}
          </Button>
        }
      />

      {renderContent()}

      {showForm && (
        <QcCheckpointFormModal
          key={editing?.checkpointId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          inspectors={inspectors}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('qc.deleteTitle')}
        message={deleteTarget ? t('qc.deleteMessage', { id: deleteTarget.checkpointId }) : ''}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        loading={deleting}
        error={deleteError || null}
        onConfirm={handleDelete}
        onClose={closeDelete}
      />
    </div>
  );
};

export default QcCheckpointList;
