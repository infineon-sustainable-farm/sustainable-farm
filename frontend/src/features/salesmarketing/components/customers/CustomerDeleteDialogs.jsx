import { useTranslation } from 'react-i18next';
import { ConfirmDialog } from '../../../../shared/ui';
import { locationOf } from './customerUtils';

/** Confirmations before deleting a customer or a contact. */
function CustomerDeleteDialogs({
  customer,
  deletingCustomer,
  onCloseCustomer,
  onConfirmCustomer,
  contact,
  deletingContact,
  onCloseContact,
  onConfirmContact,
}) {
  const { t } = useTranslation('salesmarketing');
  return (
    <>
      <ConfirmDialog
        open={Boolean(customer)}
        onClose={onCloseCustomer}
        onConfirm={onConfirmCustomer}
        loading={deletingCustomer}
        title={t('customers.deleteCustomer.title')}
        confirmLabel={deletingCustomer ? t('customers.deleting') : t('common:actions.delete')}
        message={
          customer && (
            <>
              <p>{t('customers.deleteCustomer.message', { name: customer.companyName })}</p>
              <p className="mt-2 line-clamp-3">{locationOf(customer) || t('customers.deleteCustomer.noLocation')}</p>
            </>
          )
        }
      />
      <ConfirmDialog
        open={Boolean(contact)}
        onClose={onCloseContact}
        onConfirm={onConfirmContact}
        loading={deletingContact}
        title={t('customers.deleteContact.title')}
        confirmLabel={deletingContact ? t('customers.deleting') : t('common:actions.delete')}
        message={
          contact && (
            <>
              <p>{t('customers.deleteContact.message', { name: contact.fullName })}</p>
              <p className="mt-2">{contact.email || contact.role || t('customers.deleteContact.noDetails')}</p>
            </>
          )
        }
      />
    </>
  );
}

export default CustomerDeleteDialogs;
