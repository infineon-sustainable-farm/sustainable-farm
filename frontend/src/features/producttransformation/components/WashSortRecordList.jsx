/**
 * Product Transformation - Washing & Sorting screen
 * Displays, filters, creates, edits and deletes washing/sorting records.
 *
 * Data contract: /api/product-transformation/wash-sort-records (see WashSortRecordController).
 * The backend rejects non-positive input/output quantities, negative waste or
 * water usage, inactive equipment and operators without the WASHER role.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { washSortApi } from '../api/washSortApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import ProcessFilters from './ProcessFilters';
import WashSortFormModal from './WashSortFormModal';
import WashSortTable from './WashSortTable';
import { EMPTY_PROCESS_FILTERS, filterByProcess, fromDateTimeLocal } from './processRun';

const EQUIPMENT_TYPE = 'WASHING';

const WashSortRecordList = () => {
  const { t } = useTranslation('producttransformation');
  const [records, setRecords] = useState([]);
  const [batches, setBatches] = useState([]);
  const [equipment, setEquipment] = useState([]);
  const [operators, setOperators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(EMPTY_PROCESS_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [recordData, batchData, equipmentData, operatorData] = await Promise.all([
        washSortApi.getAllWashSortRecords(),
        batchApi.getAllBatches(),
        equipmentApi.getAllEquipment(),
        operatorApi.getAllOperators(),
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
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const washEquipment = useMemo(
    () => equipment.filter((item) => item.equipmentType === EQUIPMENT_TYPE),
    [equipment],
  );

  // Every operator is used to resolve names on existing records; only WASHERs
  // can be assigned to a new record (the backend enforces that rule).
  const washOperators = useMemo(
    () => operators.filter((operator) => operator.role === 'WASHER'),
    [operators],
  );

  const filteredRecords = useMemo(() => filterByProcess(records, filters), [records, filters]);

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
    const payload = {
      inputQuantityKg: Number(form.inputQuantityKg),
      outputQuantityKg: Number(form.outputQuantityKg),
      wasteQuantityKg: Number(form.wasteQuantityKg),
      waterUsageLiters: Number(form.waterUsageLiters),
      startTime: fromDateTimeLocal(form.startTime),
      endTime: fromDateTimeLocal(form.endTime),
      equipmentId: form.equipmentId || null,
      operatorId: form.operatorId || null,
    };

    if (editing) {
      await washSortApi.updateWashSortRecord(editing.recordId, payload);
    } else {
      await washSortApi.createWashSortRecord({
        recordId: form.recordId.trim(),
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
      await washSortApi.deleteWashSortRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('washSort.deleteError'));
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
    if (loading) return <WashSortTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('washSort.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        <ProcessFilters
          filters={filters}
          onChange={updateFilter}
          batches={batches}
          equipment={washEquipment}
          count={t('common.records', { count: filteredRecords.length })}
        />
        {filteredRecords.length === 0 ? (
          <EmptyState
            message={records.length === 0 ? t('washSort.empty') : t('washSort.noMatch')}
            actionLabel={records.length === 0 ? t('washSort.new') : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        ) : (
          <WashSortTable
            records={filteredRecords}
            equipment={equipment}
            operators={operators}
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
        title={t('washSort.title')}
        subtitle={t('washSort.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('washSort.new')}
          </Button>
        }
      />

      {renderContent()}

      {showForm && (
        <WashSortFormModal
          key={editing?.recordId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          equipment={washEquipment}
          operators={washOperators}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('washSort.deleteTitle')}
        message={deleteTarget ? t('washSort.deleteMessage', { id: deleteTarget.recordId }) : ''}
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

export default WashSortRecordList;
