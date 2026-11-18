/**
 * Product Transformation - Equipment screen
 * Displays, filters, creates, edits and deletes the processing and drying
 * machines of the farm equipment register (ADR 0004, /api/equipment). A machine
 * created from Machinery shows here without type, capacity or energy until it
 * is edited.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { equipmentApi } from '../api/equipmentApi';
import EquipmentFilters from './EquipmentFilters';
import EquipmentFormModal from './EquipmentFormModal';
import EquipmentTable from './EquipmentTable';

const EMPTY_FILTERS = {
  type: 'all',
  status: 'all',
  search: '',
};

const EquipmentList = () => {
  const { t } = useTranslation('producttransformation');
  const [equipment, setEquipment] = useState([]);
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
      const data = await equipmentApi.getAllEquipment();
      setEquipment(Array.isArray(data) ? data : []);
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

  const filteredEquipment = useMemo(() => {
    const search = filters.search.trim().toLowerCase();
    return equipment
      .filter((item) => {
        if (filters.type !== 'all' && item.equipmentType !== filters.type) return false;
        if (filters.status !== 'all' && item.maintenanceStatus !== filters.status) return false;
        if (
          search &&
          !`${item.equipmentId} ${item.equipmentName} ${item.location || ''}`
            .toLowerCase()
            .includes(search)
        ) {
          return false;
        }
        return true;
      })
      .sort((a, b) => String(a.equipmentId).localeCompare(String(b.equipmentId)));
  }, [equipment, filters]);

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (item) => {
    setEditing(item);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
    const payload = {
      equipmentName: form.equipmentName.trim(),
      equipmentType: form.equipmentType,
      capacityKgPerHour: Number(form.capacityKgPerHour),
      energyConsumptionKwhPerKg: Number(form.energyConsumptionKwhPerKg),
      location: form.location.trim() || null,
      maintenanceStatus: form.maintenanceStatus,
      lastMaintenanceDate: form.lastMaintenanceDate || null,
    };

    if (editing) {
      await equipmentApi.updateEquipment(editing.equipmentId, payload);
    } else {
      await equipmentApi.createEquipment({
        equipmentId: form.equipmentId.trim(),
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
      await equipmentApi.deleteEquipment(deleteTarget.equipmentId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('equipment.deleteError'));
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
    if (loading) return <EquipmentTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('equipment.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        <EquipmentFilters filters={filters} onChange={updateFilter} count={filteredEquipment.length} />
        {filteredEquipment.length === 0 ? (
          <EmptyState
            message={equipment.length === 0 ? t('equipment.empty') : t('equipment.noMatch')}
            actionLabel={equipment.length === 0 ? t('equipment.new') : undefined}
            onAction={equipment.length === 0 ? openCreate : undefined}
          />
        ) : (
          <EquipmentTable equipment={filteredEquipment} onEdit={openEdit} onDelete={setDeleteTarget} />
        )}
      </>
    );
  };

  return (
    <div className="p-6">
      <PageHeader
        className="mb-6"
        title={t('equipment.title')}
        subtitle={t('equipment.subtitle')}
        actions={
          <Button icon={Plus} onClick={openCreate}>
            {t('equipment.new')}
          </Button>
        }
      />

      {renderContent()}

      {showForm && (
        <EquipmentFormModal
          key={editing?.equipmentId ?? 'create'}
          open
          editing={editing}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('equipment.deleteTitle')}
        message={deleteTarget ? t('equipment.deleteMessage', { id: deleteTarget.equipmentId }) : ''}
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

export default EquipmentList;
