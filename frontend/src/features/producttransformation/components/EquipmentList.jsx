/**
 * Product Transformation - Equipment screen
 * Lists and filters the processing and drying machines of the farm equipment
 * register (ADR 0004, /api/product-transformation/equipment) and edits their PT
 * specs: type, capacity, energy, location and last maintenance. Machinery owns
 * the machines: it alone creates, renames, changes the status of and deletes
 * them, so this screen sends the user there (OWNER_LINKS.equipment). A machine
 * created in Machinery shows here with N/A specs until they are edited.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { OWNER_LINKS } from '../../../app/ownerLinks';
import { EmptyState, ErrorState, OwnerHint, PageHeader } from '../../../shared/ui';
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
  const [editing, setEditing] = useState(null);

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

  const closeForm = () => setEditing(null);

  // Only the PT specs: the name and the status of a machine are changed in Machinery.
  const handleSubmit = async (form) => {
    await equipmentApi.updateEquipment(editing.equipmentId, {
      equipmentType: form.equipmentType,
      capacityKgPerHour: Number(form.capacityKgPerHour),
      energyConsumptionKwhPerKg: Number(form.energyConsumptionKwhPerKg),
      location: form.location.trim() || null,
      lastMaintenanceDate: form.lastMaintenanceDate || null,
    });
    closeForm();
    await load();
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
          <EmptyState message={equipment.length === 0 ? t('equipment.empty') : t('equipment.noMatch')} />
        ) : (
          <EquipmentTable equipment={filteredEquipment} onEdit={setEditing} />
        )}
        <OwnerHint
          owner={OWNER_LINKS.equipment}
          variant={equipment.length === 0 ? 'empty' : 'more'}
          className="mt-4"
        />
      </>
    );
  };

  return (
    <div className="p-6">
      <PageHeader className="mb-6" title={t('equipment.title')} subtitle={t('equipment.subtitle')} />

      {renderContent()}

      {editing && (
        <EquipmentFormModal
          key={editing.equipmentId}
          open
          editing={editing}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}
    </div>
  );
};

export default EquipmentList;
