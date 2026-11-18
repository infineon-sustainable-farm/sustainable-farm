/**
 * Product Transformation - Drying screen
 * Displays, filters, creates, edits and deletes drying runs.
 *
 * Data contract: /api/product-transformation/drying-runs (see DryingRunController).
 * The backend enforces a 6–18 % end-moisture range for EU compliance, plus
 * active drying equipment and operators with the DRYER role.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { dryingApi } from '../api/dryingApi';
import { batchApi } from '../api/batchApi';
import { equipmentApi } from '../api/equipmentApi';
import { operatorApi } from '../api/operatorApi';
import ProcessFilters from './ProcessFilters';
import DryingRunFormModal from './DryingRunFormModal';
import DryingRunTable from './DryingRunTable';
import { EMPTY_PROCESS_FILTERS, filterByProcess, fromDateTimeLocal } from './processRun';

const DRYER_TYPES = ['DRYING', 'SOLAR_DRYER', 'TUNNEL_DRYER', 'TESTA_DRYER'];

const DryingRunList = () => {
  const { t } = useTranslation('producttransformation');
  const [runs, setRuns] = useState([]);
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

  // Every operator is used to resolve names on existing runs; only DRYERs
  // can be assigned to a new run (the backend enforces that rule).
  const dryerOperators = useMemo(
    () => operators.filter((operator) => operator.role === 'DRYER'),
    [operators],
  );

  const filteredRuns = useMemo(() => filterByProcess(runs, filters), [runs, filters]);

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (run) => {
    setEditing(run);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
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

    closeForm();
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await dryingApi.deleteDryingRun(deleteTarget.runId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('drying.deleteError'));
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
    if (loading) return <DryingRunTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('drying.loadError')}
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
          equipment={dryers}
          count={t('drying.runsCount', { count: filteredRuns.length })}
        />
        {filteredRuns.length === 0 ? (
          <EmptyState
            message={runs.length === 0 ? t('drying.empty') : t('drying.noMatch')}
            actionLabel={runs.length === 0 ? t('drying.new') : undefined}
            onAction={runs.length === 0 ? openCreate : undefined}
          />
        ) : (
          <DryingRunTable
            runs={filteredRuns}
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
        title={t('drying.title')}
        subtitle={t('drying.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('drying.new')}
          </Button>
        }
      />

      {renderContent()}

      {showForm && (
        <DryingRunFormModal
          key={editing?.runId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          equipment={dryers}
          operators={dryerOperators}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('drying.deleteTitle')}
        message={deleteTarget ? t('drying.deleteMessage', { id: deleteTarget.runId }) : ''}
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

export default DryingRunList;
