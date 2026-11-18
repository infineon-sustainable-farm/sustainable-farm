import { Plus, Search } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Select } from '../../../../shared/ui';
import { STATUS_OPTIONS, useCustomerLabels } from './customerUtils';

/** Search, status filter and Add customer of the CRM header. */
function CustomerToolbar({ searchQuery, onSearchChange, statusFilter, onStatusChange, onAdd }) {
  const { t } = useTranslation('salesmarketing');
  const labels = useCustomerLabels();
  return (
    <div className="flex w-full flex-col items-center gap-3 sm:flex-row md:w-auto">
      <div className="relative w-full sm:w-80">
        <Search
          size={18}
          aria-hidden="true"
          className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-text-muted"
        />
        <Input
          type="text"
          className="pl-10"
          aria-label={t('customers.toolbar.search')}
          placeholder={t('customers.toolbar.searchPlaceholder')}
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>
      <Select
        className="sm:w-44"
        aria-label={t('customers.toolbar.statusFilter')}
        value={statusFilter}
        onChange={(e) => onStatusChange(e.target.value)}
      >
        <option value="all">{t('customers.toolbar.allStatuses')}</option>
        {STATUS_OPTIONS.map((status) => (
          <option key={status} value={status}>
            {labels.status(status)}
          </option>
        ))}
      </Select>
      <Button icon={Plus} onClick={onAdd} className="shrink-0">
        {t('customers.toolbar.add')}
      </Button>
    </div>
  );
}

export default CustomerToolbar;
