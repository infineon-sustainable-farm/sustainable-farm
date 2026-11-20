import { Pencil } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IconButton, StatusBadge, Table } from '../../../shared/ui';
import { formatIsoDate, formatNumber, notAvailable } from '../utils/format';
import { equipmentTypeLabel, maintenanceStatusLabel, maintenanceStatusTone } from './equipmentOptions';

/**
 * Table of the PT equipment. A machine registered from Machinery without its PT attributes shows
 * N/A for its type, capacity, energy and location. The only row action edits the PT specs: the
 * machine itself is managed in Machinery.
 */
export default function EquipmentTable({ equipment = [], onEdit, loading = false }) {
  const { t } = useTranslation('producttransformation');

  const columns = [
    { key: 'equipmentId', header: t('equipment.columns.equipment'), className: 'whitespace-nowrap' },
    { key: 'equipmentName', header: t('equipment.columns.name'), className: 'whitespace-nowrap' },
    {
      key: 'equipmentType',
      header: t('equipment.columns.type'),
      className: 'whitespace-nowrap',
      cell: (row) => equipmentTypeLabel(row.equipmentType),
    },
    {
      key: 'capacity',
      header: t('equipment.columns.capacity'),
      className: 'whitespace-nowrap',
      cell: (row) => t('equipment.units.kgPerHour', { value: formatNumber(row.capacityKgPerHour, 1) }),
    },
    {
      key: 'energy',
      header: t('equipment.columns.energy'),
      className: 'whitespace-nowrap',
      cell: (row) => t('equipment.units.kwhPerKg', { value: formatNumber(row.energyConsumptionKwhPerKg, 2) }),
    },
    {
      key: 'location',
      header: t('equipment.columns.location'),
      className: 'whitespace-nowrap',
      cell: (row) => row.location || notAvailable(),
    },
    {
      key: 'status',
      header: t('equipment.columns.status'),
      cell: (row) => (
        <StatusBadge
          tone={maintenanceStatusTone(row.maintenanceStatus)}
          label={maintenanceStatusLabel(row.maintenanceStatus)}
        />
      ),
    },
    {
      key: 'lastMaintenance',
      header: t('equipment.columns.lastMaintenance'),
      className: 'whitespace-nowrap',
      cell: (row) => formatIsoDate(row.lastMaintenanceDate),
    },
  ];

  return (
    <Table
      caption={t('equipment.title')}
      columns={columns}
      rows={equipment}
      getRowKey={(row) => row.equipmentId}
      loading={loading}
      rowActions={(item) => (
        <IconButton icon={Pencil} label={t('equipment.editSpecs')} onClick={() => onEdit(item)} />
      )}
    />
  );
}
