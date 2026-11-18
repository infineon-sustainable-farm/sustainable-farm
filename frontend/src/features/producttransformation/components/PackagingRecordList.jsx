/**
 * Product Transformation - Packaging screen
 * Displays, filters, creates, edits and deletes packaging records.
 *
 * Data contract: /api/packaging-records (see PackagingRecordController).
 * Business rules enforced by the backend: lot codes are mandatory and unique,
 * quantity must be positive, and export readiness is tracked per record.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { packagingApi } from '../api/packagingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import FormAlert from './FormAlert';
import PackagingFilters from './PackagingFilters';
import PackagingFormModal from './PackagingFormModal';
import PackagingSummary from './PackagingSummary';
import PackagingTable from './PackagingTable';
import { PACKAGE_TYPES } from './packagingOptions';

const EMPTY_FILTERS = {
  batchId: 'all',
  packageType: 'all',
  exportReady: 'all',
  date: '',
};

const PackagingRecordList = ({ onOpenCompliance }) => {
  const { t } = useTranslation('producttransformation');
  const [records, setRecords] = useState([]);
  const [batches, setBatches] = useState([]);
  const [equipment, setEquipment] = useState([]);
  const [operators, setOperators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
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
    // Deferred so the initial setState happens outside the effect body.
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const noValue = t('packaging.noValue');

  const equipmentName = useMemo(() => {
    const byId = new Map(equipment.map((item) => [item.equipmentId, item.equipmentName]));
    return (equipmentId) => byId.get(equipmentId) || equipmentId || noValue;
  }, [equipment, noValue]);

  const operatorName = useMemo(() => {
    const byId = new Map(operators.map((operator) => [operator.operatorId, operator.operatorName]));
    return (operatorId) => byId.get(operatorId) || operatorId || noValue;
  }, [operators, noValue]);

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

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setActionError('');
    setShowForm(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    setActionError('');
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
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

    closeForm();
    await load();
  };

  const handleMarkExportReady = async (record) => {
    setActionError('');
    try {
      await packagingApi.markExportReady(record.recordId);
      await load();
    } catch (err) {
      setActionError(err?.message || t('packaging.markReadyError'));
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await packagingApi.deletePackagingRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('packaging.deleteError'));
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
    if (loading) return <PackagingTable loading equipmentName={equipmentName} operatorName={operatorName} />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('packaging.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        {actionError && <FormAlert className="mb-4">{actionError}</FormAlert>}
        <PackagingSummary summary={packageSummary} />
        <PackagingFilters filters={filters} onChange={updateFilter} batches={batches} count={filteredRecords.length} />
        {filteredRecords.length === 0 ? (
          <EmptyState
            message={records.length === 0 ? t('packaging.empty') : t('packaging.noMatch')}
            actionLabel={records.length === 0 ? t('packaging.new') : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        ) : (
          <PackagingTable
            records={filteredRecords}
            equipmentName={equipmentName}
            operatorName={operatorName}
            onOpenCompliance={onOpenCompliance}
            onMarkExportReady={handleMarkExportReady}
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
        title={t('packaging.title')}
        subtitle={t('packaging.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('packaging.new')}
          </Button>
        }
      />

      {renderContent()}

      {showForm && (
        <PackagingFormModal
          key={editing?.recordId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          equipment={equipment}
          operators={operators}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('packaging.deleteTitle')}
        message={deleteTarget ? t('packaging.deleteMessage', { id: deleteTarget.recordId }) : ''}
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

export default PackagingRecordList;
