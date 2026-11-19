import { CalendarDays } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../../shared/ui';
import { useSalesFormat } from '../../format';

const MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

/** Months (1 to 12) of a season from its first to its last month, across the new year if needed. */
function seasonMonths(start, end) {
  if (!start || !end) return [];
  const months = [];
  for (let month = start; ; month = (month % 12) + 1) {
    months.push(month);
    if (month === end || months.length === 12) return months;
  }
}

/** A variety name as compared: no accents, no surrounding spaces, lower case ("Amélie" is "amelie"). */
const nameKey = (name) =>
  String(name ?? '')
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .trim()
    .toLowerCase();

/**
 * Harvest months of the varieties the products are made of (product.variety, the catalogue
 * name), read from the plants variety catalogue (harvestStartMonth, harvestEndMonth).
 */
function harvestMonths(products, varieties) {
  const names = new Set(products.map((p) => nameKey(p.variety)).filter(Boolean));
  const months = new Set();
  varieties
    .filter((v) => names.has(nameKey(v.varietyName)))
    .forEach((v) => seasonMonths(v.harvestStartMonth, v.harvestEndMonth).forEach((m) => months.add(m)));
  return months;
}

/** Runs of consecutive months, for example [[2, 7]] for February to July. */
function monthRuns(months) {
  const runs = [];
  MONTHS.forEach((month) => {
    if (!months.has(month)) return;
    const last = runs[runs.length - 1];
    if (last && last[1] === month - 1) last[1] = month;
    else runs.push([month, month]);
  });
  return runs;
}

/** The harvest months of the products on a 12 month bar, from the plants variety catalogue. */
function HarvestWindow({ products = [], varieties = [] }) {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  const months = harvestMonths(products, varieties);
  const monthName = (month) => format.date(new Date(2000, month - 1, 1), { month: 'long' });
  const label = monthRuns(months)
    .map(([from, to]) => (from === to ? monthName(from) : t('dashboard.harvest.range', { from: monthName(from), to: monthName(to) })))
    .join(', ');

  return (
    <Card className="flex items-center gap-4">
      <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-pill bg-success-bg text-success-fg">
        <CalendarDays size={20} aria-hidden="true" />
      </span>
      <div className="flex-1">
        <div className="mb-1 flex items-end justify-between">
          <h2 className="text-lg font-semibold">{t('dashboard.harvest.title')}</h2>
          <span className="text-sm text-text-muted">{label || t('dashboard.harvest.none')}</span>
        </div>
        <div className="flex h-3 w-full overflow-hidden rounded-pill bg-neutral-bg">
          {MONTHS.map((month) => (
            <div key={month} className={`h-full flex-1 ${months.has(month) ? 'bg-success-fg' : ''}`} />
          ))}
        </div>
      </div>
    </Card>
  );
}

export default HarvestWindow;
