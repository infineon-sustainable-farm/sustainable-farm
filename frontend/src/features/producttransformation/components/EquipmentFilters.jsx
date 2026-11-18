import { Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Field, Input, Select } from '../../../shared/ui';
import FilterBar from './FilterBar';
import { EQUIPMENT_TYPES, MAINTENANCE_STATUSES, equipmentTypeLabel, maintenanceStatusLabel } from './equipmentOptions';

/** Type, maintenance status and text search filters of the equipment list. */
export default function EquipmentFilters({ filters, onChange, count }) {
  const { t } = useTranslation('producttransformation');
  const update = (key) => (event) => onChange(key, event.target.value);

  return (
    <FilterBar count={t('equipment.count', { count })}>
      <Field label={t('equipment.type')}>
        <Select aria-label={t('equipment.filterType')} value={filters.type} onChange={update('type')}>
          <option value="all">{t('equipment.allTypes')}</option>
          {EQUIPMENT_TYPES.map((type) => (
            <option key={type} value={type}>
              {equipmentTypeLabel(type)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('equipment.status')}>
        <Select aria-label={t('equipment.filterStatus')} value={filters.status} onChange={update('status')}>
          <option value="all">{t('equipment.allStatuses')}</option>
          {MAINTENANCE_STATUSES.map((status) => (
            <option key={status} value={status}>
              {maintenanceStatusLabel(status)}
            </option>
          ))}
        </Select>
      </Field>

      <Field label={t('equipment.search')}>
        <div className="relative">
          <Search
            size={16}
            aria-hidden="true"
            className="absolute top-1/2 left-3 -translate-y-1/2 text-text-muted"
          />
          <Input
            type="search"
            className="pl-9"
            aria-label={t('equipment.searchLabel')}
            placeholder={t('equipment.searchPlaceholder')}
            value={filters.search}
            onChange={update('search')}
          />
        </div>
      </Field>
    </FilterBar>
  );
}
