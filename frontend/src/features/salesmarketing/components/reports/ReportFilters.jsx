import { List, Map as MapIcon, Search, TrendingUp, TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Input } from '../../../../shared/ui';
import FilterMenu from './FilterMenu';
import { STATUS_OPTIONS } from './reportUtils';

const FILTERS = [
  { id: 'all', labelKey: 'reports.filters.all', icon: List },
  { id: 'forecasting', labelKey: 'reports.filters.forecasting', icon: TrendingUp },
  { id: 'regional', labelKey: 'reports.filters.regional', icon: MapIcon },
  { id: 'risk', labelKey: 'reports.filters.risk', icon: TriangleAlert },
];

/** Category buttons, search field and status menu of the reports screen. */
function ReportFilters({ category, onCategoryChange, search, onSearchChange, status, onStatusChange }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
      <div
        role="group"
        aria-label={t('reports.filters.label')}
        className="flex flex-wrap items-center gap-1 rounded-tile bg-neutral-bg p-1"
      >
        {FILTERS.map(({ id, labelKey, icon: Icon }) => {
          const selected = category === id;
          return (
            <button
              key={id}
              type="button"
              aria-pressed={selected}
              onClick={() => onCategoryChange(id)}
              className={`flex min-h-9 cursor-pointer items-center gap-2 rounded-control px-3 py-1 text-sm ${
                selected ? 'bg-surface-raised font-semibold text-text-accent shadow-raised' : 'text-text-muted hover:text-text'
              }`}
            >
              <Icon size={16} aria-hidden="true" />
              {t(labelKey)}
            </button>
          );
        })}
      </div>
      <div className="flex flex-1 items-center gap-4">
        <div className="relative max-w-md flex-1">
          <Search size={18} aria-hidden="true" className="absolute top-1/2 left-3 -translate-y-1/2 text-text-muted" />
          <Input
            type="text"
            aria-label={t('reports.searchLabel')}
            placeholder={t('reports.searchPlaceholder')}
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            className="pl-10"
          />
        </div>
        <FilterMenu
          label={t('reports.filtersButton')}
          heading={t('reports.statusHeading')}
          highlighted={status !== 'all'}
          value={status}
          onChange={onStatusChange}
          options={['all', ...STATUS_OPTIONS].map((value) => ({
            value,
            label: value === 'all' ? t('reports.allStatuses') : t(`reports.statusOption.${value}`),
          }))}
        />
      </div>
    </div>
  );
}

export default ReportFilters;
