import { EllipsisVertical, Pencil, Trash2, UserPlus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IconButton } from '../../../../shared/ui';

const ITEM =
  'flex w-full items-center gap-2 px-3 py-2 text-left text-sm transition duration-(--duration-fast) ease-standard hover:bg-surface-muted';

/** Edit, Add contact and Delete of one customer row, opened by the row's menu button. */
function CustomerRowMenu({ customer, open, onToggle, onEdit, onAddContact, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <div className="relative flex justify-end">
      <IconButton
        icon={EllipsisVertical}
        tone="neutral"
        label={t('customers.list.actionsFor', { name: customer.companyName })}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={(e) => {
          e.stopPropagation();
          onToggle();
        }}
      />
      {open && (
        <div
          role="menu"
          onClick={(e) => e.stopPropagation()}
          className="absolute top-full right-0 z-(--z-overlay) mt-1 min-w-44 rounded-control border border-border-subtle bg-surface-raised py-1 shadow-overlay"
        >
          <button type="button" role="menuitem" className={ITEM} onClick={onEdit}>
            <Pencil size={16} aria-hidden="true" className="text-text-muted" />
            {t('customers.list.editCustomer')}
          </button>
          <button type="button" role="menuitem" className={ITEM} onClick={onAddContact}>
            <UserPlus size={16} aria-hidden="true" className="text-text-muted" />
            {t('customers.list.addContact')}
          </button>
          <button type="button" role="menuitem" className={`${ITEM} text-error-fg`} onClick={onDelete}>
            <Trash2 size={16} aria-hidden="true" />
            {t('customers.list.deleteCustomer')}
          </button>
        </div>
      )}
    </div>
  );
}

export default CustomerRowMenu;
