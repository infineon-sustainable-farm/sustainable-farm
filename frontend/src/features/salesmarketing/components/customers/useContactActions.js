import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/api';
import { emptyContactForm, getErrorMessage } from './customerUtils';

/** Add, edit and delete of the contacts of the selected customer, with their dialogs' state. */
export function useContactActions({ effectiveSelectedId, flashSuccess, flashError, setActionError, fetchCustomersData }) {
  const { t } = useTranslation('salesmarketing');
  const [showContactModal, setShowContactModal] = useState(false);
  const [editingContactId, setEditingContactId] = useState(null);
  const [contactForm, setContactForm] = useState(emptyContactForm());
  const [savingContact, setSavingContact] = useState(false);
  const [pendingDeleteContact, setPendingDeleteContact] = useState(null);
  const [deletingContact, setDeletingContact] = useState(false);

  /** Opens the Add Contact dialog for the given customer, the selected one by default. */
  const openCreateContact = (customerId = effectiveSelectedId) => {
    if (customerId == null) {
      flashError(t('customers.errors.selectCustomerFirst'));
      return;
    }
    setContactForm(emptyContactForm(customerId));
    setEditingContactId(null);
    setShowContactModal(true);
    setActionError(null);
  };

  const openEditContact = (contact) => {
    setContactForm({
      customerId: contact.customerId,
      fullName: contact.fullName || '',
      email: contact.email || '',
      phone: contact.phone || '',
      role: contact.role || '',
      isPrimary: !!contact.isPrimary,
    });
    setEditingContactId(contact.contactId);
    setShowContactModal(true);
    setActionError(null);
  };

  const handleSaveContact = async (e) => {
    e.preventDefault();
    setSavingContact(true);
    setActionError(null);
    try {
      const payload = {
        customerId: contactForm.customerId,
        fullName: contactForm.fullName,
        email: contactForm.email || null,
        phone: contactForm.phone || null,
        role: contactForm.role || null,
        isPrimary: contactForm.isPrimary,
      };
      if (editingContactId != null) {
        await api.updateCustomerContact(editingContactId, payload);
        flashSuccess(t('customers.messages.contactUpdated', { name: payload.fullName }));
      } else {
        await api.createCustomerContact(payload);
        flashSuccess(t('customers.messages.contactAdded', { name: payload.fullName }));
      }
      setShowContactModal(false);
      await fetchCustomersData();
    } catch (err) {
      flashError(
        getErrorMessage(
          err,
          editingContactId != null ? t('customers.errors.updateContactFailed') : t('customers.errors.createContactFailed')
        )
      );
    } finally {
      setSavingContact(false);
    }
  };

  const confirmDeleteContact = async () => {
    if (!pendingDeleteContact) return;
    setDeletingContact(true);
    try {
      await api.deleteCustomerContact(pendingDeleteContact.contactId);
      const name = pendingDeleteContact.fullName;
      setPendingDeleteContact(null);
      flashSuccess(t('customers.messages.contactDeleted', { name }));
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, t('customers.errors.deleteContactFailed')));
    } finally {
      setDeletingContact(false);
    }
  };

  return {
    openCreateContact,
    openEditContact,
    contactModal: {
      open: showContactModal,
      editing: editingContactId != null,
      form: contactForm,
      setForm: setContactForm,
      saving: savingContact,
      onClose: () => setShowContactModal(false),
      onSubmit: handleSaveContact,
    },
    pendingDeleteContact,
    setPendingDeleteContact,
    deletingContact,
    confirmDeleteContact,
  };
}
