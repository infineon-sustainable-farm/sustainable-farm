import { Pencil, Trash2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, IconButton, Table } from '../../../../../shared/ui'
import { useWaterFormat } from '../../../format'
import { LocationCell } from './LocationCell'
import { parseFieldCoordinates } from './coordinates'

function useCommonCells() {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  return {
    t,
    text: (value) => value || format.none,
    area: (row) => t('farms.hectares', { value: format.number(row.areaHectares ?? 0) }),
  }
}

/** Farms with their edit, delete and "show its fields" actions. */
export function FarmsTable({ rows, selectedFarm, onEdit, onDelete, onToggleFields }) {
  const { t, text, area } = useCommonCells()
  const columns = [
    { key: 'name', header: t('farms.columns.name'), sortable: true },
    { key: 'address', header: t('farms.columns.address'), cell: (r) => text(r.address) },
    { key: 'areaHectares', header: t('farms.columns.area'), sortable: true, cell: area },
    { key: 'description', header: t('farms.columns.description'), cell: (r) => text(r.description) },
    { key: 'latitude', header: t('farms.columns.location'), cell: (r) => <LocationCell latitude={r.latitude} longitude={r.longitude} /> },
  ]
  return (
    <Table
      caption={t('farms.farmsTitle')}
      columns={columns}
      rows={rows}
      rowActions={(row) => (
        <div className="flex items-center justify-end gap-1">
          <IconButton icon={Pencil} label={t('common.edit')} onClick={() => onEdit(row)} />
          <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => onDelete(row)} />
          <Button size="sm" variant="ghost" onClick={() => onToggleFields(row.id)}>
            {selectedFarm === row.id ? t('farms.showAll') : t('farms.showFields')}
          </Button>
        </div>
      )}
    />
  )
}

/** Fields, filtered by the selected farm. */
export function FieldsTable({ rows, onDelete }) {
  const { t, text, area } = useCommonCells()
  const columns = [
    { key: 'name', header: t('farms.columns.name'), sortable: true },
    { key: 'cropType', header: t('farms.columns.crop'), cell: (r) => text(r.cropType) },
    { key: 'soilType', header: t('farms.columns.soil'), cell: (r) => text(r.soilType) },
    { key: 'areaHectares', header: t('farms.columns.area'), cell: area },
    {
      key: 'coordinates',
      header: t('farms.columns.location'),
      cell: (r) => {
        const position = parseFieldCoordinates(r.coordinates)
        return position ? <LocationCell {...position} /> : text(r.coordinates)
      },
    },
  ]
  return (
    <Table
      caption={t('farms.fieldsTitle')}
      columns={columns}
      rows={rows}
      rowActions={(row) => <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => onDelete(row)} />}
    />
  )
}

/** Irrigation zones. */
export function ZonesTable({ rows, onDelete }) {
  const { t, text, area } = useCommonCells()
  const columns = [
    { key: 'name', header: t('farms.columns.name'), sortable: true },
    { key: 'irrigationMethod', header: t('farms.columns.irrigationMethod'), cell: (r) => text(r.irrigationMethod) },
    { key: 'areaHectares', header: t('farms.columns.area'), cell: area },
  ]
  return (
    <Table
      caption={t('farms.zonesTitle')}
      columns={columns}
      rows={rows}
      rowActions={(row) => <IconButton icon={Trash2} tone="danger" label={t('common.delete')} onClick={() => onDelete(row)} />}
    />
  )
}
