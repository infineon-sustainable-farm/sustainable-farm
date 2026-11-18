import { Mail, Pencil, Phone, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, IconButton } from '../../../../shared/ui';
import { initialOf } from './customerUtils';

/** One contact of the selected customer, with edit and delete. */
function ContactCard({ contact, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <li className="rounded-control border border-border-subtle bg-surface-muted p-3">
      <div className="mb-1 flex items-start justify-between">
        <div className="flex items-center gap-2">
          <div
            aria-hidden="true"
            className="flex h-8 w-8 items-center justify-center rounded-pill bg-neutral-bg text-sm font-medium text-neutral-fg"
          >
            {initialOf(contact.fullName)}
          </div>
          <div>
            <p className="flex items-center gap-1 text-sm">
              {contact.fullName}
              {contact.isPrimary && <Badge tone="success">{t('customers.contacts.primary')}</Badge>}
            </p>
            <p className="text-xs font-medium text-text-accent">{contact.role || t('customers.contacts.roleFallback')}</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <IconButton icon={Pencil} tone="neutral" label={t('customers.contacts.edit')} onClick={() => onEdit(contact)} />
          <IconButton icon={Trash2} tone="danger" label={t('customers.contacts.delete')} onClick={() => onDelete(contact)} />
        </div>
      </div>
      {contact.email && (
        <p className="mt-1 flex items-center gap-1 text-sm">
          <Mail size={16} aria-hidden="true" className="shrink-0 text-text-muted" />
          <a href={`mailto:${contact.email}`} className="truncate text-text-accent hover:underline">
            {contact.email}
          </a>
        </p>
      )}
      {contact.phone && (
        <p className="mt-1 flex items-center gap-1 text-sm">
          <Phone size={16} aria-hidden="true" className="shrink-0 text-text-muted" />
          <a href={`tel:${contact.phone}`} className="truncate">
            {contact.phone}
          </a>
        </p>
      )}
    </li>
  );
}

export default ContactCard;
