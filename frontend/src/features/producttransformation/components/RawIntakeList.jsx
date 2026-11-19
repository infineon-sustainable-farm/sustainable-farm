/**
 * Product Transformation - Raw Intake screen
 * Displays, filters, creates, edits and deletes raw intake (reception) records.
 *
 * Data contract: /api/product-transformation/raw-intakes (see RawIntakeController).
 * The backend enforces positive received quantities; IDs and batch links are
 * immutable after creation.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { useCreateParam } from '../../../shared/hooks/useCreateParam';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { batchApi } from '../api/batchApi';
import { varietyApi } from '../api/varietyApi';
import { varietyKeyOf } from './rawIntakeOptions';
import RawIntakeFilters from './RawIntakeFilters';
import RawIntakeFormModal from './RawIntakeFormModal';
import RawIntakeTable from './RawIntakeTable';

const EMPTY_FILTERS = {
  batchId: 'all',
  grade: 'all',
  variety: 'all',
  date: '',
};

const RawIntakeList = () => {
  const { t } = useTranslation('producttransformation');
  const [intakes, setIntakes] = useState([]);
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // The plants variety catalogue, loaded apart: the list still shows when it fails.
  const [varieties, setVarieties] = useState({ items: [], loading: true, error: null });

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  // "?create=1": another module sent the user here to receive a batch (app/ownerLinks.js).
  const [createRequested, clearCreateRequest] = useCreateParam();
  const formOpen = showForm || createRequested;

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

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

  const loadVarieties = useCallback(async () => {
    setVarieties((current) => ({ ...current, loading: true, error: null }));
    try {
      const data = await varietyApi.getVarieties();
      setVarieties({ items: Array.isArray(data) ? data : [], loading: false, error: null });
    } catch (err) {
      setVarieties({ items: [], loading: false, error: err });
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  useEffect(() => {
    const timeoutId = window.setTimeout(loadVarieties, 0);
    return () => window.clearTimeout(timeoutId);
  }, [loadVarieties]);

  const filteredIntakes = useMemo(() => {
    return intakes
      .filter((intake) => {
        if (filters.batchId !== 'all' && intake.batchId !== filters.batchId) return false;
        if (filters.grade !== 'all' && intake.receivedGrade !== filters.grade) return false;
        if (filters.variety !== 'all' && varietyKeyOf(intake.receivedVariety) !== filters.variety) {
          return false;
        }
        if (filters.date && String(intake.intakeDate || '').slice(0, 10) !== filters.date)
          return false;
        return true;
      })
      .sort((a, b) => String(b.intakeDate || '').localeCompare(String(a.intakeDate || '')));
  }, [intakes, filters]);

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (intake) => {
    setEditing(intake);
    setShowForm(true);
  };

  const closeForm = () => {
    clearCreateRequest();
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
    const payload = {
      sourceFarm: form.sourceFarm.trim(),
      sourceBlock: form.sourceBlock.trim(),
      intakeDate: form.intakeDate,
      receivedQuantityKg: Number(form.receivedQuantityKg),
      receivedVariety: form.receivedVariety,
      receivedGrade: form.receivedGrade,
      intakeOperatorPersonId: form.intakeOperatorPersonId ? Number(form.intakeOperatorPersonId) : null,
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

    closeForm();
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await rawIntakeApi.deleteRawIntake(deleteTarget.intakeId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('rawIntake.deleteError'));
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
    if (loading) return <RawIntakeTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('rawIntake.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        <RawIntakeFilters
          filters={filters}
          onChange={updateFilter}
          batches={batches}
          varieties={varieties.items}
          count={filteredIntakes.length}
        />
        {filteredIntakes.length === 0 ? (
          <EmptyState
            message={intakes.length === 0 ? t('rawIntake.empty') : t('rawIntake.noMatch')}
            actionLabel={intakes.length === 0 ? t('rawIntake.new') : undefined}
            onAction={intakes.length === 0 ? openCreate : undefined}
          />
        ) : (
          <RawIntakeTable intakes={filteredIntakes} onEdit={openEdit} onDelete={setDeleteTarget} />
        )}
      </>
    );
  };

  return (
    <div className="p-6">
      <PageHeader
        className="mb-6"
        title={t('rawIntake.title')}
        subtitle={t('rawIntake.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('rawIntake.new')}
          </Button>
        }
      />

      {renderContent()}

      {formOpen && (
        <RawIntakeFormModal
          key={editing?.intakeId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          varieties={varieties}
          onRetryVarieties={loadVarieties}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('rawIntake.deleteTitle')}
        message={deleteTarget ? t('rawIntake.deleteMessage', { id: deleteTarget.intakeId }) : ''}
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

export default RawIntakeList;
