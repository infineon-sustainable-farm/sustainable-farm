import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { PageHeader } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import ContactFormModal from '../components/customers/ContactFormModal';
import { useContactActions } from '../components/customers/useContactActions';
import CustomerDeleteDialogs from '../components/customers/CustomerDeleteDialogs';
import CustomerDetailPanel from '../components/customers/CustomerDetailPanel';
import CustomerFormModal from '../components/customers/CustomerFormModal';
import CustomerList from '../components/customers/CustomerList';
import CustomerStats from '../components/customers/CustomerStats';
import CustomerToolbar from '../components/customers/CustomerToolbar';
import LogisticsHub from '../components/customers/LogisticsHub';
import {
  emptyCustomerForm,
  filterCustomers,
  getErrorMessage,
  locationOf,
  normalizeStatus,
} from '../components/customers/customerUtils';
import { orderVolumeByCustomer, useCustomersData } from '../components/customers/useCustomersData';

export default function CustomersPage() {
  const { t } = useTranslation('salesmarketing');
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedId, setSelectedId] = useState(null);
  const [openMenuCustomerId, setOpenMenuCustomerId] = useState(null);
  const [sortAscending, setSortAscending] = useState(true);
  const [customerPage, setCustomerPage] = useState(1);

  const [showCustomerModal, setShowCustomerModal] = useState(false);
  const [editingCustomerId, setEditingCustomerId] = useState(null);
  const [customerForm, setCustomerForm] = useState(emptyCustomerForm());
  const [savingCustomer, setSavingCustomer] = useState(false);


  const [pendingDeleteCustomer, setPendingDeleteCustomer] = useState(null);
  const [deletingCustomer, setDeletingCustomer] = useState(false);

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuCustomerId(null);
    if (openMenuCustomerId != null) {
      window.addEventListener('click', handleClickOutside);
      return () => window.removeEventListener('click', handleClickOutside);
    }
  }, [openMenuCustomerId]);

  // The messages hide after a delay; the pending timers stop when the screen closes.
  const flashTimers = useRef([]);
  useEffect(() => () => flashTimers.current.forEach((id) => window.clearTimeout(id)), []);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    flashTimers.current.push(window.setTimeout(() => setActionSuccess(null), 4000));
  };

  const flashError = useCallback((msg) => {
    setActionError(msg);
    setActionSuccess(null);
    flashTimers.current.push(window.setTimeout(() => setActionError(null), 6000));
  }, []);

  const {
    loading,
    setLoading,
    apiError,
    customers,
    orders,
    contacts,
    certifications,
    customerCerts,
    setCustomerCerts,
    fetchCustomersData,
    fetchCustomerCertifications,
  } = useCustomersData({ statusFilter, setSelectedId, flashError });

  const { filteredCustomers, visibleCustomers, customerPageCount, currentCustomerPage } = filterCustomers(customers, {
    searchQuery,
    statusFilter,
    sortAscending,
    page: customerPage,
  });

  const effectiveSelectedId = filteredCustomers.some((c) => c.customerId === selectedId)
    ? selectedId
    : filteredCustomers[0]?.customerId ?? null;

  useEffect(() => {
    if (effectiveSelectedId == null || customerCerts[effectiveSelectedId] || customers.length === 0) {
      return undefined;
    }
    let active = true;
    api
      .getCustomerCertifications(effectiveSelectedId)
      .then((res) => {
        if (!active) return;
        setCustomerCerts((prev) => ({ ...prev, [effectiveSelectedId]: Array.isArray(res) ? res : [] }));
      })
      .catch((err) => {
        if (!active) return;
        setCustomerCerts((prev) => ({ ...prev, [effectiveSelectedId]: [] }));
        flashError(getErrorMessage(err, t('customers.errors.certificationsLoadFailed')));
      });
    return () => {
      active = false;
    };
  }, [effectiveSelectedId, customerCerts, customers.length, flashError, setCustomerCerts, t]);

  const selectedCustomer =
    filteredCustomers.find((c) => c.customerId === effectiveSelectedId) ||
    customers.find((c) => c.customerId === effectiveSelectedId) ||
    null;
  const selectedContacts = selectedCustomer ? contacts.filter((c) => c.customerId === selectedCustomer.customerId) : [];
  const selectedCustomerCerts = (effectiveSelectedId && customerCerts[effectiveSelectedId]) || [];
  const volumes = orderVolumeByCustomer(orders, new Date().getFullYear());
  const countOf = (status) => customers.filter((c) => normalizeStatus(c.status) === status).length;

  const openCreateCustomer = () => {
    setCustomerForm(emptyCustomerForm());
    setEditingCustomerId(null);
    setShowCustomerModal(true);
    setActionError(null);
  };

  const openEditCustomer = async (customer) => {
    setActionError(null);
    setSavingCustomer(true);
    try {
      const res = await api.getCustomerById(customer.customerId);
      const c = res || customer;
      setCustomerForm({
        companyName: c.companyName || '',
        customerCode: c.customerCode || '',
        customerType: c.customerType || 'distributor',
        country: c.country || '',
        region: c.region || '',
        city: c.city || '',
        status: c.status || 'prospect',
        paymentTerms: c.paymentTerms || '',
      });
      setEditingCustomerId(customer.customerId);
      setShowCustomerModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, t('customers.errors.loadForEditFailed', { id: customer.customerId })));
    } finally {
      setSavingCustomer(false);
    }
  };

  const handleSaveCustomer = async (e) => {
    e.preventDefault();
    setSavingCustomer(true);
    setActionError(null);
    try {
      const payload = {
        companyName: customerForm.companyName,
        customerCode: customerForm.customerCode || null,
        customerType: customerForm.customerType,
        country: customerForm.country || null,
        region: customerForm.region || null,
        city: customerForm.city || null,
        status: customerForm.status,
        paymentTerms: customerForm.paymentTerms || null,
      };
      if (editingCustomerId != null) {
        await api.updateCustomer(editingCustomerId, payload);
        flashSuccess(t('customers.messages.customerUpdated', { name: payload.companyName }));
      } else {
        const created = await api.createCustomer(payload);
        flashSuccess(t('customers.messages.customerCreated', { name: payload.companyName }));
        if (created?.customerId) setSelectedId(created.customerId);
      }
      setShowCustomerModal(false);
      await fetchCustomersData();
    } catch (err) {
      flashError(
        getErrorMessage(
          err,
          editingCustomerId != null ? t('customers.errors.updateCustomerFailed') : t('customers.errors.createCustomerFailed')
        )
      );
    } finally {
      setSavingCustomer(false);
    }
  };

  const confirmDeleteCustomer = async () => {
    if (!pendingDeleteCustomer) return;
    setDeletingCustomer(true);
    try {
      await api.deleteCustomer(pendingDeleteCustomer.customerId);
      const name = pendingDeleteCustomer.companyName;
      setPendingDeleteCustomer(null);
      flashSuccess(t('customers.messages.customerDeleted', { name }));
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, t('customers.errors.deleteCustomerFailed')));
    } finally {
      setDeletingCustomer(false);
    }
  };

  const {
    openCreateContact,
    openEditContact,
    contactModal,
    pendingDeleteContact,
    setPendingDeleteContact,
    deletingContact,
    confirmDeleteContact,
  } = useContactActions({ effectiveSelectedId, flashSuccess, flashError, setActionError, fetchCustomersData });

  const toggleCertification = async (certId) => {
    if (!effectiveSelectedId) return;
    const alreadyLinked = selectedCustomerCerts.some((cc) => cc.certificationId === certId);
    try {
      if (alreadyLinked) {
        await api.unlinkCustomerCertification(effectiveSelectedId, certId);
        flashSuccess(t('customers.messages.certificationUnlinked'));
      } else {
        await api.linkCustomerCertification(effectiveSelectedId, certId);
        flashSuccess(t('customers.messages.certificationLinked'));
      }
      await fetchCustomerCertifications(effectiveSelectedId);
    } catch (err) {
      flashError(getErrorMessage(err, t('customers.errors.certificationUpdateFailed')));
    }
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title={t('customers.title')}
        actions={
          <CustomerToolbar
            searchQuery={searchQuery}
            onSearchChange={(value) => {
              setSearchQuery(value);
              setCustomerPage(1);
            }}
            statusFilter={statusFilter}
            onStatusChange={(value) => {
              setStatusFilter(value);
              setCustomerPage(1);
              setLoading(true);
            }}
            onAdd={openCreateCustomer}
          />
        }
      />

      {apiError && (
        <Notice tone="offline" title={t('customers.apiNotice')} onRetry={fetchCustomersData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" title={t('customers.actionFailed')} onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      <CustomerStats
        activeCount={countOf('active')}
        prospectCount={countOf('prospect')}
        inactiveCount={countOf('inactive')}
        totalCount={customers.length}
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <CustomerList
          className="lg:col-span-2"
          loading={loading}
          customers={visibleCustomers}
          matchingCount={filteredCustomers.length}
          totalCount={customers.length}
          selectedId={effectiveSelectedId}
          volumes={volumes}
          sortAscending={sortAscending}
          onToggleSort={() => setSortAscending((value) => !value)}
          page={currentCustomerPage}
          pageCount={customerPageCount}
          onPageChange={setCustomerPage}
          openMenuId={openMenuCustomerId}
          onToggleMenu={(id) => setOpenMenuCustomerId(openMenuCustomerId === id ? null : id)}
          onSelect={setSelectedId}
          onEdit={(customer) => {
            setOpenMenuCustomerId(null);
            setSelectedId(customer.customerId);
            openEditCustomer(customer);
          }}
          onAddContact={(customer) => {
            setOpenMenuCustomerId(null);
            setSelectedId(customer.customerId);
            openCreateContact(customer.customerId);
          }}
          onDelete={(customer) => {
            setOpenMenuCustomerId(null);
            setPendingDeleteCustomer(customer);
          }}
        />
        <div className="flex flex-col gap-4">
          <CustomerDetailPanel
            customer={selectedCustomer}
            certifications={certifications}
            linkedCerts={selectedCustomerCerts}
            contacts={selectedContacts}
            onEdit={openEditCustomer}
            onRefreshCertifications={fetchCustomerCertifications}
            onToggleCertification={toggleCertification}
            onAddContact={() => openCreateContact()}
            onEditContact={openEditContact}
            onDeleteContact={setPendingDeleteContact}
          />
          <LogisticsHub location={locationOf(selectedCustomer)} />
        </div>
      </div>

      <CustomerFormModal
        open={showCustomerModal}
        editingId={editingCustomerId}
        form={customerForm}
        setForm={setCustomerForm}
        saving={savingCustomer}
        onClose={() => setShowCustomerModal(false)}
        onSubmit={handleSaveCustomer}
      />
      <ContactFormModal {...contactModal} />
      <CustomerDeleteDialogs
        customer={pendingDeleteCustomer}
        deletingCustomer={deletingCustomer}
        onCloseCustomer={() => setPendingDeleteCustomer(null)}
        onConfirmCustomer={confirmDeleteCustomer}
        contact={pendingDeleteContact}
        deletingContact={deletingContact}
        onCloseContact={() => setPendingDeleteContact(null)}
        onConfirmContact={confirmDeleteContact}
      />
    </div>
  );
}
