import { Filter } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card } from '../../../shared/ui';

/**
 * Filter card above a Product Transformation list: the "Filters" label, the filter fields passed
 * as children, and the record count on the right.
 */
export default function FilterBar({ count, children, showLabel = true }) {
  const { t } = useTranslation('producttransformation');
  return (
    <Card className="mb-6" padding="none">
      <div className="flex flex-wrap items-end gap-4 p-4">
        <div className="flex items-center gap-2 self-center text-text-muted">
          <Filter size={18} aria-hidden="true" />
          {showLabel && <span className="text-sm font-medium">{t('common.filters')}</span>}
        </div>
        {children}
        {count != null && <span className="ml-auto self-center text-sm text-text-muted">{count}</span>}
      </div>
    </Card>
  );
}
