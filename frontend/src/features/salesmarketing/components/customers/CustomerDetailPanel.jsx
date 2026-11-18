import { Check, MapPin, Pencil, RefreshCw, User, UserPlus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, Button, Card, EmptyState, IconButton } from '../../../../shared/ui';
import ContactCard from './ContactCard';
import CustomerStatusBadge from './CustomerStatusBadge';
import { initialOf, locationOf, useCustomerLabels } from './customerUtils';

const SECTION_TITLE = 'text-xs font-semibold text-text-muted';

function Certifications({ certifications, linkedCerts, onRefresh, onToggle }) {
  const { t } = useTranslation('salesmarketing');
  return (
    <div className="mb-4">
      <div className="mb-2 flex items-center justify-between">
        <h3 className={SECTION_TITLE}>{t('customers.detail.certifications')}</h3>
        <IconButton icon={RefreshCw} label={t('customers.detail.refreshCertifications')} onClick={onRefresh} />
      </div>
      <div className="flex flex-wrap gap-2">
        {certifications.map((cert) => {
          const linked = linkedCerts.some((cc) => cc.certificationId === cert.certificationId);
          return (
            <button
              key={cert.certificationId}
              type="button"
              aria-pressed={linked}
              onClick={() => onToggle(cert.certificationId)}
              className={`inline-flex items-center gap-1 rounded-pill border px-3 py-1 text-xs font-medium transition duration-(--duration-fast) ease-standard ${
                linked
                  ? 'border-primary bg-primary-tint text-text-accent'
                  : 'border-border-subtle bg-neutral-bg text-neutral-fg hover:bg-surface-muted'
              }`}
            >
              {linked && <Check size={16} aria-hidden="true" />}
              {cert.name}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/** Selected customer: identity, certifications to link or unlink, contacts. */
function CustomerDetailPanel({
  customer,
  certifications,
  linkedCerts,
  contacts,
  onEdit,
  onRefreshCertifications,
  onToggleCertification,
  onAddContact,
  onEditContact,
  onDeleteContact,
}) {
  const { t } = useTranslation('salesmarketing');
  const labels = useCustomerLabels();

  if (!customer) {
    return (
      <Card className="flex min-h-100 items-center justify-center">
        <EmptyState icon={User} message={t('customers.detail.selectPrompt')} />
      </Card>
    );
  }

  const location = locationOf(customer);

  return (
    <Card className="flex min-h-100 flex-col">
      <div className="mb-6 flex items-start justify-between">
        <div
          aria-hidden="true"
          className="flex h-16 w-16 items-center justify-center rounded-card bg-primary text-3xl font-bold text-on-primary shadow-raised"
        >
          {initialOf(customer.companyName)}
        </div>
        <Button variant="ghost" size="sm" icon={Pencil} onClick={() => onEdit(customer)}>
          {t('customers.detail.edit')}
        </Button>
      </div>
      <div className="mb-4">
        <h2 className="mb-1 text-2xl font-bold">{customer.companyName}</h2>
        <p className="flex items-center gap-1 text-text-muted">
          <MapPin size={16} aria-hidden="true" />
          {location || t('customers.detail.locationUnavailable')}
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <CustomerStatusBadge status={customer.status} />
          <Badge size="md">{labels.type(customer.customerType)}</Badge>
          {customer.paymentTerms && <Badge size="md">{labels.paymentTerms(customer.paymentTerms)}</Badge>}
          <span className="text-xs text-text-muted">{customer.customerCode || `#${customer.customerId}`}</span>
        </div>
      </div>

      {certifications.length > 0 && (
        <Certifications
          certifications={certifications}
          linkedCerts={linkedCerts}
          onRefresh={() => onRefreshCertifications(customer.customerId)}
          onToggle={onToggleCertification}
        />
      )}

      <div className="mt-auto">
        <div className="mb-2 flex items-center justify-between">
          <h3 className={SECTION_TITLE}>{t('customers.contacts.title', { count: contacts.length })}</h3>
          <Button variant="ghost" size="sm" icon={UserPlus} onClick={onAddContact}>
            {t('customers.contacts.add')}
          </Button>
        </div>
        {contacts.length === 0 ? (
          <p className="text-sm text-text-muted italic">{t('customers.contacts.empty')}</p>
        ) : (
          <ul className="flex flex-col gap-3">
            {contacts.map((contact) => (
              <ContactCard key={contact.contactId} contact={contact} onEdit={onEditContact} onDelete={onDeleteContact} />
            ))}
          </ul>
        )}
      </div>
    </Card>
  );
}

export default CustomerDetailPanel;
