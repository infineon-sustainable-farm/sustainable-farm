import { Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, Input } from '../../../../shared/ui';
import FilterMenu from '../reports/FilterMenu';

const MENU_FILTERS = ['active', 'high', 'medium', 'data', 'resolved', 'all'];
const CHIPS = [
  { id: 'active', selected: 'bg-text text-on-primary' },
  { id: 'high', selected: 'bg-danger text-on-primary font-semibold' },
  { id: 'medium', selected: 'bg-warning-bg text-warning-fg font-semibold' },
  { id: 'data', selected: 'bg-primary text-on-primary font-semibold' },
];

/** Search field, filter menu and counted filter chips of the alert list. */
function AlertControls({ search, onSearchChange, filter, onFilterChange, counts }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <Card className="flex flex-col items-center justify-between gap-4 sm:flex-row">
      <div className="flex w-full items-center gap-4 sm:w-auto">
        <div className="relative w-full sm:w-64">
          <Search size={18} aria-hidden="true" className="absolute top-1/2 left-3 -translate-y-1/2 text-text-muted" />
          <Input
            type="text"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            aria-label={t('alerts.searchLabel')}
            placeholder={t('alerts.searchPlaceholder')}
            className="pl-10"
          />
        </div>
        <FilterMenu
          iconOnly
          align="left"
          label={t('alerts.chooseFilter')}
          highlighted={filter === 'resolved'}
          value={filter}
          onChange={onFilterChange}
          options={MENU_FILTERS.map((value) => ({ value, label: t(`alerts.menu.${value}`) }))}
        />
      </div>
      <div role="group" aria-label={t('alerts.chipsLabel')} className="flex w-full flex-wrap items-center gap-2 sm:w-auto">
        {CHIPS.map((chip) => (
          <button
            key={chip.id}
            type="button"
            aria-pressed={filter === chip.id}
            onClick={() => onFilterChange(chip.id)}
            className={`min-h-9 cursor-pointer rounded-pill px-4 py-1 text-sm whitespace-nowrap ${
              filter === chip.id ? chip.selected : 'bg-neutral-bg text-neutral-fg hover:bg-surface-muted'
            }`}
          >
            {t(`alerts.chips.${chip.id}`, { count: counts[chip.id] })}
          </button>
        ))}
      </div>
    </Card>
  );
}

export default AlertControls;
