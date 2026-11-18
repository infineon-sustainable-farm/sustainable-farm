import { ArrowDown, ArrowUp, Package, UserSearch } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, EmptyState, IconButton, Pagination, Skeleton } from '../../../../shared/ui';
import { useSalesFormat } from '../../format';
import CustomerRowMenu from './CustomerRowMenu';
import CustomerStatusBadge from './CustomerStatusBadge';
import { initialOf, normalizeStatus, useCustomerLabels } from './customerUtils';

const HEADER = 'text-xs font-medium text-text-muted';

function CustomerRow({ customer, selected, volume, menuOpen, onSelect, menuHandlers }) {
  const { t } = useTranslation('salesmarketing');
  const format = useSalesFormat();
  const labels = useCustomerLabels();
  const isInactive = normalizeStatus(customer.status) === 'inactive';

  return (
    <li
      onClick={onSelect}
      aria-current={selected ? 'true' : undefined}
      className={`grid cursor-pointer grid-cols-12 items-center gap-4 border-b border-border-subtle px-5 py-3 transition duration-(--duration-fast) ease-standard hover:bg-surface-muted ${
        selected ? 'bg-surface-muted' : isInactive ? 'bg-error-tint' : ''
      }`}
    >
      <div className="col-span-11 flex min-w-0 items-center gap-3 sm:col-span-4">
        {customer.avatarUrl ? (
          <img className="h-10 w-10 shrink-0 rounded-pill object-cover" alt="" src={customer.avatarUrl} />
        ) : (
          <div
            className={`relative flex h-10 w-10 shrink-0 items-center justify-center rounded-pill font-semibold ${
              selected ? 'bg-primary text-on-primary' : 'bg-neutral-bg text-text'
            }`}
            aria-hidden="true"
          >
            {initialOf(customer.companyName)}
            {isInactive && (
              <span className="absolute top-0 right-0 h-3 w-3 rounded-pill border-2 border-surface-raised bg-danger" />
            )}
          </div>
        )}
        <div className="flex min-w-0 flex-col">
          <button
            type="button"
            className="truncate text-left font-semibold"
            onClick={(e) => {
              e.stopPropagation();
              onSelect();
            }}
          >
            {customer.companyName}
          </button>
          <span className="truncate text-xs text-text-muted">
            {t('customers.list.id', { code: customer.customerCode || `CUST-${customer.customerId}` })}
          </span>
        </div>
      </div>

      <div className="col-span-2 hidden items-center gap-2 text-sm sm:flex">
        <span title={customer.country || ''}>{labels.country(customer.country)}</span>
        <span className="text-text-muted">{customer.region || '-'}</span>
      </div>

      <div className="col-span-3 hidden flex-col md:flex">
        {volume != null ? (
          <>
            <span className="font-semibold">
              {t('customers.list.volume', { value: format.number(volume, { maximumFractionDigits: 2 }) })}
            </span>
            <span className="flex items-center gap-1 text-xs text-text-accent">
              <Package size={16} aria-hidden="true" />
              {t('customers.list.ytdVolume')}
            </span>
          </>
        ) : (
          <>
            <span className="font-semibold text-text-muted">{t('customers.list.zeroVolume')}</span>
            <span className="text-xs text-text-muted">{t('customers.list.noOrders')}</span>
          </>
        )}
      </div>

      <div className="col-span-2 hidden justify-end sm:flex">
        <CustomerStatusBadge status={customer.status} />
      </div>

      <div className="col-span-1">
        <CustomerRowMenu customer={customer} open={menuOpen} {...menuHandlers} />
      </div>
    </li>
  );
}

/**
 * Customer list of the CRM: sortable by name, 8 per page, a click selects the customer for the
 * detail panel.
 */
function CustomerList({
  loading,
  customers,
  matchingCount,
  totalCount,
  selectedId,
  volumes,
  sortAscending,
  onToggleSort,
  page,
  pageCount,
  onPageChange,
  openMenuId,
  onToggleMenu,
  onSelect,
  onEdit,
  onAddContact,
  onDelete,
  className,
}) {
  const { t } = useTranslation('salesmarketing');

  return (
    <Card padding="none" className={`flex flex-col overflow-hidden ${className ?? ''}`}>
      <div className="grid grid-cols-12 items-center gap-4 border-b border-border-divider bg-surface-muted px-5 py-3">
        <div className="col-span-4 flex items-center gap-2">
          <span className={HEADER}>{t('customers.list.customerId')}</span>
          <IconButton
            icon={sortAscending ? ArrowUp : ArrowDown}
            tone="neutral"
            label={sortAscending ? t('customers.list.sortDescending') : t('customers.list.sortAscending')}
            onClick={onToggleSort}
          />
        </div>
        <span className={`col-span-2 hidden sm:block ${HEADER}`}>{t('customers.list.region')}</span>
        <span className={`col-span-3 hidden md:block ${HEADER}`}>{t('customers.list.orderVolume')}</span>
        <span className={`col-span-2 hidden text-right sm:block ${HEADER}`}>{t('customers.list.status')}</span>
        <span className={`col-span-8 text-right sm:col-span-1 ${HEADER}`}>{t('customers.list.action')}</span>
      </div>

      <div className="h-150 overflow-y-auto">
        {loading ? (
          <div aria-busy="true" className="space-y-3 p-5">
            <p role="status" className="text-sm text-text-muted">
              {t('customers.list.loading')}
            </p>
            {[0, 1, 2, 3, 4].map((index) => (
              <Skeleton key={index} className="h-12 w-full" />
            ))}
          </div>
        ) : matchingCount === 0 ? (
          <EmptyState icon={UserSearch} title={t('customers.list.emptyTitle')} message={t('customers.list.emptyMessage')} />
        ) : (
          <ul>
            {customers.map((customer) => (
              <CustomerRow
                key={customer.customerId}
                customer={customer}
                selected={customer.customerId === selectedId}
                volume={volumes[customer.customerId]}
                menuOpen={openMenuId === customer.customerId}
                onSelect={() => onSelect(customer.customerId)}
                menuHandlers={{
                  onToggle: () => onToggleMenu(customer.customerId),
                  onEdit: () => onEdit(customer),
                  onAddContact: () => onAddContact(customer),
                  onDelete: () => onDelete(customer),
                }}
              />
            ))}
          </ul>
        )}
      </div>

      <div className="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-border-divider px-5 py-3">
        <span className="text-sm text-text-muted">
          {t('customers.list.showing', { shown: customers.length, matching: matchingCount, total: totalCount })}
        </span>
        <Pagination
          page={page - 1}
          totalPages={pageCount}
          onPageChange={(next) => onPageChange(next + 1)}
          label={t('customers.list.pagination')}
        />
      </div>
    </Card>
  );
}

export default CustomerList;
