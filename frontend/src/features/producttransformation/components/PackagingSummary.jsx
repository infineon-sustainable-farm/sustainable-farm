import { useTranslation } from 'react-i18next';
import { Card } from '../../../shared/ui';
import { formatWeight } from '../utils/format';
import { packageLabel } from './packagingOptions';

/** Packaged quantity and record count per package type, empty types included. */
export default function PackagingSummary({ summary }) {
  const { t } = useTranslation('producttransformation');
  return (
    <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      {summary.map((item) => (
        <Card key={item.type} as="div" padding="none" className="p-4">
          <p className="text-xs font-medium text-text-muted">{packageLabel(item.type)}</p>
          <p className="mt-1 text-2xl font-bold">{formatWeight(item.totalKg)}</p>
          <p className="text-sm text-text-muted">{t('packaging.recordCount', { count: item.count })}</p>
        </Card>
      ))}
    </div>
  );
}
