import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

const MAP_IMAGE =
  "url('https://lh3.googleusercontent.com/aida-public/AB6AXuCdzyBuOs41VqgnR7KtckWRpmdf8cvjvxFy-8_cxwfEULsaP_vcPpVJW49lLZZtOTqnsSvL_KFOYTV0zmV9EL3z4t6YY4-gVn6TjaD3EUf-dRL-T2bVlXXdwPjg1Y5TNWr9lx-NUxGgfe9nOEu0doLqb7jpmYfFPKI1DYWkC0-1lwUTxiC4A7ohqLZ3YgYyKsghwHYk3xKwanUUen5WYGhkYujt45XBcJyve8j0qlHSKfMopljBSXeR')";

const CUSTOMER_TYPES = ['distributor', 'retailer', 'direct_export', 'other'];
const STATUS_OPTIONS = ['active', 'inactive', 'prospect'];
const PAYMENT_TERMS = ['Net 15', 'Net 30', 'Net 45', 'Net 60', 'Due on Receipt', 'Advance Payment'];

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.validationErrors && typeof data.validationErrors === 'object') {
      const details = Object.values(data.validationErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

function countryLabel(country) {
  if (!country) return '';

  const c = country.toLowerCase().trim();

  if (c.includes('germany') || c === 'de') return 'Germany';
  if (c.includes('burkina')) return 'Burkina Faso';
  if (c.includes('france') || c === 'fr') return 'France';
  if (c.includes('netherlands') || c === 'nl') return 'Netherlands';

  return country;
}

function initialOf(name = '') {
  return (name.trim()[0] || '?').toUpperCase();
}

function normalizeStatus(status) {
  return (status || 'prospect').toLowerCase();
}

function StatusBadge({ status }) {
  const s = normalizeStatus(status);
  if (s === 'active') {
    return (
      <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-[#E6F4EA] text-[#137333] font-label-md text-label-md">
        Active
      </span>
    );
  }
  if (s === 'inactive') {
    return (
      <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-[#FCE8E6] text-[#C5221F] font-label-md text-label-md">
        Inactive
      </span>
    );
  }
  return (
    <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-secondary/10 text-secondary font-label-md text-label-md">
      Prospect
    </span>
  );
}

function emptyCustomerForm() {
  return {
    companyName: '',
    customerCode: '',
    customerType: 'distributor',
    country: 'Germany',
    region: '',
    city: '',
    status: 'prospect',
    paymentTerms: 'Net 30',
  };
}

function emptyContactForm(customerId = null) {
  return {
    customerId,
    fullName: '',
    email: '',
    phone: '',
    role: '',
    isPrimary: false,
  };
}

export default function CustomersPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [contacts, setContacts] = useState([]);
  const [certifications, setCertifications] = useState([]);
  const [customerCerts, setCustomerCerts] = useState({});
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

  const [showContactModal, setShowContactModal] = useState(false);
  const [editingContactId, setEditingContactId] = useState(null);
  const [contactForm, setContactForm] = useState(emptyContactForm());
  const [savingContact, setSavingContact] = useState(false);

  const [pendingDeleteCustomer, setPendingDeleteCustomer] = useState(null);
  const [deletingCustomer, setDeletingCustomer] = useState(false);
  const [pendingDeleteContact, setPendingDeleteContact] = useState(null);
  const [deletingContact, setDeletingContact] = useState(false);

  useEffect(() => {
    fetchCustomersData();
  }, [statusFilter]);

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuCustomerId(null);
    if (openMenuCustomerId != null) {
      window.addEventListener('click', handleClickOutside);
      return () => window.removeEventListener('click', handleClickOutside);
    }
  }, [openMenuCustomerId]);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
    window.setTimeout(() => setActionError(null), 6000);
  };

  const fetchCustomersData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [customerRes, orderRes, contactRes, certRes] = await Promise.allSettled([
  statusFilter === 'all' ? api.getCustomers() : api.getCustomers(statusFilter),
  api.getOrders(),
  api.getCustomerContacts(),
  api.getCertifications(),
]);

      const failures = [];

      if (customerRes.status === 'fulfilled') {
        const list = Array.isArray(customerRes.value.data) ? customerRes.value.data : [];
        setCustomers(list);
        if (list.length > 0 && selectedId == null) {
          setSelectedId(list[0].customerId);
        }
      } else {
        setCustomers([]);
        failures.push(`Customers: ${getErrorMessage(customerRes.reason)}`);
      }if (orderRes.status === 'fulfilled') {
  setOrders(Array.isArray(orderRes.value.data) ? orderRes.value.data : []);
} else {
  setOrders([]);
  failures.push(`Orders: ${getErrorMessage(orderRes.reason)}`);
}

      if (contactRes.status === 'fulfilled') {
        setContacts(Array.isArray(contactRes.value.data) ? contactRes.value.data : []);
      } else {
        setContacts([]);
        failures.push(`Contacts: ${getErrorMessage(contactRes.reason)}`);
      }

      if (certRes.status === 'fulfilled') {
        setCertifications(Array.isArray(certRes.value.data) ? certRes.value.data : []);
      } else {
        setCertifications([]);
        failures.push(`Certifications: ${getErrorMessage(certRes.reason)}`);
      }

      if (failures.length > 0 && customerRes.status === 'rejected') {
        setApiError(
          'Backend REST API at http://localhost:8080/api is offline or unreachable. Customer data is unavailable. ' +
            failures.join(' - ')
        );
      } else if (failures.length > 0) {
        setApiError(`Partial load - ${failures.join(" - ")}`);
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to Spring Boot backend API at http://localhost:8080/api'));
      setCustomers([]);
      setContacts([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchCustomerCertifications = async (customerId) => {
    try {
      const res = await api.getCustomerCertifications(customerId);
      setCustomerCerts((prev) => ({ ...prev, [customerId]: Array.isArray(res.data) ? res.data : [] }));
    } catch (err) {
      setCustomerCerts((prev) => ({ ...prev, [customerId]: [] }));
      flashError(getErrorMessage(err, 'Failed to load customer certifications'));
    }
  };

  useEffect(() => {
    if (selectedId != null && !customerCerts[selectedId] && customers.length > 0) {
      fetchCustomerCertifications(selectedId);
    }
  }, [selectedId, customers.length]);

  const displayCustomers = customers;

  const q = searchQuery.trim().toLowerCase();
  const filteredCustomers = displayCustomers.filter((c) => {
    if (statusFilter !== 'all' && normalizeStatus(c.status) !== normalizeStatus(statusFilter)) {
      return false;
    }
    if (!q) return true;
    const haystack = [
      c.companyName,
      c.customerCode,
      c.region,
      c.city,
      c.country,
      c.status,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return haystack.includes(q);
  });

  const sortedCustomers = [...filteredCustomers].sort((a, b) => {
    const left = (a.companyName || '').toLowerCase();
    const right = (b.companyName || '').toLowerCase();
    return sortAscending ? left.localeCompare(right) : right.localeCompare(left);
  });
  const CUSTOMER_PAGE_SIZE = 8;
  const customerPageCount = Math.max(1, Math.ceil(sortedCustomers.length / CUSTOMER_PAGE_SIZE));
  const visibleCustomers = sortedCustomers.slice(
    (customerPage - 1) * CUSTOMER_PAGE_SIZE,
    customerPage * CUSTOMER_PAGE_SIZE
  );

  useEffect(() => {
    setCustomerPage(1);
  }, [searchQuery, statusFilter]);

  useEffect(() => {
    if (customerPage > customerPageCount) setCustomerPage(customerPageCount);
  }, [customerPage, customerPageCount]);

  useEffect(() => {
    if (filteredCustomers.length === 0) {
      setSelectedId(null);
      return;
    }
    const stillVisible = filteredCustomers.some((c) => c.customerId === selectedId);
    if (!stillVisible) {
      setSelectedId(filteredCustomers[0].customerId);
    }
  }, [filteredCustomers, selectedId]);

  const selectedCustomer =
    filteredCustomers.find((c) => c.customerId === selectedId) ||
    displayCustomers.find((c) => c.customerId === selectedId) ||
    null;

  const selectedContacts = selectedCustomer
    ? contacts.filter((c) => c.customerId === selectedCustomer.customerId)
    : [];
  const selectedContact = selectedContacts.find((c) => c.isPrimary) || selectedContacts[0] || null;

  const selectedCustomerCerts = (selectedId && customerCerts[selectedId]) || [];
  const matchedCertNames = certifications.filter((cert) =>
    selectedCustomerCerts.some((cc) => cc.certificationId === cert.certificationId)
  );
  const currentYear = new Date().getFullYear();

const orderVolumeByCustomer = orders.reduce((acc, order) => {
  const customerId = Number(order.customerId);
  const orderDate = order.orderDate;

  if (!customerId || !orderDate) {
    return acc;
  }

  const date = new Date(orderDate);

  if (date.getFullYear() !== currentYear) {
    return acc;
  }

  const volumeKg = Number(order.totalVolumeKg ?? 0);

  if (!Number.isFinite(volumeKg)) {
    return acc;
  }

  acc[customerId] = (acc[customerId] || 0) + volumeKg;

  return acc;
}, {});
  const activeCount = displayCustomers.filter((c) => normalizeStatus(c.status) === 'active').length;
  const prospectCount = displayCustomers.filter((c) => normalizeStatus(c.status) === 'prospect').length;
  const inactiveCount = displayCustomers.filter((c) => normalizeStatus(c.status) === 'inactive').length;
  const totalCount = displayCustomers.length;

  const locationLine = selectedCustomer
    ? [selectedCustomer.city, selectedCustomer.region, selectedCustomer.country].filter(Boolean).join(', ')
    : '';

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
      const c = res.data || customer;
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
      flashError(getErrorMessage(err, `Unable to load customer #${customer.customerId} for editing.`));
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
        flashSuccess(`Customer "${payload.companyName}" updated successfully.`);
      } else {
        const created = await api.createCustomer(payload);
        flashSuccess(`Customer "${payload.companyName}" created successfully.`);
        if (created.data?.customerId) {
          setSelectedId(created.data.customerId);
        }
      }
      setShowCustomerModal(false);
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, editingCustomerId != null ? 'Failed to update customer' : 'Failed to create customer'));
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
      flashSuccess(`Customer "${name}" deleted successfully.`);
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete customer'));
    } finally {
      setDeletingCustomer(false);
    }
  };

  const openCreateContact = () => {
    if (selectedId == null) {
      flashError('Please select a customer first.');
      return;
    }
    setContactForm(emptyContactForm(selectedId));
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
        flashSuccess(`Contact "${payload.fullName}" updated successfully.`);
      } else {
        await api.createCustomerContact(payload);
        flashSuccess(`Contact "${payload.fullName}" added successfully.`);
      }
      setShowContactModal(false);
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, editingContactId != null ? 'Failed to update contact' : 'Failed to create contact'));
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
      flashSuccess(`Contact "${name}" deleted successfully.`);
      await fetchCustomersData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete contact'));
    } finally {
      setDeletingContact(false);
    }
  };

  const toggleCertification = async (certId) => {
    if (!selectedId) return;
    const alreadyLinked = selectedCustomerCerts.some((cc) => cc.certificationId === certId);
    try {
      if (alreadyLinked) {
        await api.unlinkCustomerCertification(selectedId, certId);
        flashSuccess('Certification unlinked.');
      } else {
        await api.linkCustomerCertification(selectedId, certId);
        flashSuccess('Certification linked.');
      }
      await fetchCustomerCertifications(selectedId);
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to update certification'));
    }
  };

  return (
    <div className="flex flex-col w-full h-full relative">
      <div className="px-margin-desktop py-xl flex flex-col md:flex-row md:items-end justify-between gap-lg relative z-10">
        <div className="flex flex-col gap-sm">
          <span className="font-mono-label text-mono-label text-primary uppercase tracking-widest">[ CRM_MODULE ]</span>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">Customers</h2>
        </div>
        <div className="flex flex-col sm:flex-row items-center gap-md w-full md:w-auto">
          <div className="relative w-full sm:w-80 group">
            <span className="material-symbols-outlined absolute left-md top-1/2 -translate-y-1/2 text-on-surface-variant z-10 transition-colors group-focus-within:text-primary">
              search
            </span>
            <input
              className="w-full h-12 pl-12 pr-md bg-surface-container-lowest text-on-surface font-body-md text-body-md rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-outline"
              placeholder="Search by name, ID or region..."
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <select
            value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            className="h-12 px-lg bg-surface-container-low text-on-surface font-label-md rounded-lg shadow-sm border border-outline-variant/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            <option value="all">All Statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s.charAt(0).toUpperCase() + s.slice(1)}
              </option>
            ))}
          </select>
          <button
            onClick={openCreateCustomer}
            className="h-12 px-xl bg-primary text-on-primary hover:bg-on-primary-fixed-variant font-label-md text-label-md rounded-lg shadow-md hover:shadow-lg transition-all flex items-center gap-sm shrink-0"
          >
            <span className="material-symbols-outlined text-[20px]">add</span>
            ADD CUSTOMER
          </button>
        </div>
      </div>

      <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-gradient-to-bl from-primary/5 via-primary/2 to-transparent rounded-full blur-3xl pointer-events-none -z-10 translate-x-1/4 -translate-y-1/4" />

      <div className="px-margin-desktop pb-xxl grid grid-cols-12 gap-gutter relative z-10">
        {apiError && (
          <div className="col-span-12 bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
              <div>
                <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
                <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
              </div>
            </div>
            <button
              onClick={fetchCustomersData}
              className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
            >
              <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
            </button>
          </div>
        )}

        {actionError && (
          <div className="col-span-12 bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-error text-[24px]">error</span>
              <div>
                <p className="font-label-md text-label-md text-on-error-container font-bold">Action failed</p>
                <p className="font-body-sm text-body-sm text-on-error-container">{actionError}</p>
              </div>
            </div>
            <button onClick={() => setActionError(null)} className="text-on-error-container px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {actionSuccess && (
          <div className="col-span-12 bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-[#2e7d32] text-[24px]">check_circle</span>
              <p className="font-body-sm text-body-sm text-[#1b5e20]">{actionSuccess}</p>
            </div>
            <button onClick={() => setActionSuccess(null)} className="text-[#1b5e20] px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {/* Stats Row */}
        <div className="col-span-12 grid grid-cols-1 md:grid-cols-4 gap-gutter mb-lg">
          <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group">
            <div className="absolute -right-xl -top-xl w-32 h-32 bg-surface-container rounded-full opacity-50 group-hover:scale-110 transition-transform duration-500" />
            <div className="flex items-start justify-between relative z-10">
              <div className="flex flex-col gap-unit">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Total Active</span>
                <span className="font-headline-lg text-headline-lg text-on-surface">
                  {activeCount.toLocaleString()}
                </span>
              </div>
              <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary">groups</span>
              </div>
            </div>
            <div className="mt-md flex items-center gap-xs text-primary relative z-10">
              <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
              <span className="font-label-md text-label-md">12% vs last month</span>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group">
            <div className="absolute -right-xl -top-xl w-32 h-32 bg-surface-container rounded-full opacity-50 group-hover:scale-110 transition-transform duration-500" />
            <div className="flex items-start justify-between relative z-10">
              <div className="flex flex-col gap-unit">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">New Onboarded</span>
                <span className="font-headline-lg text-headline-lg text-on-surface">
                  {prospectCount.toLocaleString()}
                </span>
              </div>
              <div className="w-10 h-10 rounded-full bg-secondary/10 flex items-center justify-center">
                <span className="material-symbols-outlined text-secondary">person_add</span>
              </div>
            </div>
            <div className="mt-md flex items-center gap-xs text-primary relative z-10">
              <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
              <span className="font-label-md text-label-md">5% vs last month</span>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-lg shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group">
            <div className="absolute -right-xl -top-xl w-32 h-32 bg-surface-container rounded-full opacity-50 group-hover:scale-110 transition-transform duration-500" />
            <div className="flex items-start justify-between relative z-10">
              <div className="flex flex-col gap-unit">
                <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Inactive</span>
                <span className="font-headline-lg text-headline-lg text-on-surface">
                  {inactiveCount.toLocaleString()}
                </span>
              </div>
              <div className="w-10 h-10 rounded-full bg-error/10 flex items-center justify-center">
                <span className="material-symbols-outlined text-error">person_off</span>
              </div>
            </div>
            <div className="mt-md flex items-center gap-xs text-error relative z-10">
              <span className="material-symbols-outlined text-[16px]">info</span>
              <span className="font-label-md text-label-md">Follow-up recommended</span>
            </div>
          </div>

          <div className="col-span-1 bg-primary rounded-xl p-lg shadow-sm text-on-primary flex flex-col justify-between relative overflow-hidden">
            <div className="absolute right-0 bottom-0 translate-x-1/4 translate-y-1/4 opacity-10 pointer-events-none">
              <svg fill="currentColor" height="200" viewBox="0 0 24 24" width="200">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
              </svg>
            </div>
            <div className="flex items-center gap-sm mb-md relative z-10">
              <span className="material-symbols-outlined">insights</span>
              <span className="font-label-md text-label-md uppercase tracking-wider">AI Insight</span>
            </div>
            <p className="font-body-lg text-body-lg text-on-primary/90 relative z-10">
              {totalCount === 0
                ? 'No customer records are available yet. Create a customer to begin managing CRM relationships.'
                : `${activeCount} active customer${activeCount === 1 ? '' : 's'} and ${prospectCount} prospect${prospectCount === 1 ? '' : 's'} in the current CRM data.`}
            </p>
          </div>
        </div>

        {/* Customer List */}
        <div className="col-span-12 lg:col-span-8 bg-surface-container-lowest rounded-xl shadow-sm flex flex-col overflow-hidden relative">
          <div className="bg-surface-container-low px-lg py-md grid grid-cols-12 gap-md items-center sticky top-0 z-20">
            <div className="col-span-4 flex items-center gap-sm">
              <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Customer / ID</span>
              <button
                type="button"
                onClick={() => setSortAscending((value) => !value)}
                title={`Sort ${sortAscending ? 'descending' : 'ascending'}`}
                className="text-outline hover:text-primary transition-colors"
              >
                <span className="material-symbols-outlined text-[16px]">
                  {sortAscending ? 'arrow_upward' : 'arrow_downward'}
                </span>
              </button>
            </div>
            <div className="col-span-2 hidden sm:block">
              <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Region</span>
            </div>
            <div className="col-span-3 hidden md:block">
              <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Order Vol (YTD)</span>
            </div>
            <div className="col-span-2 hidden sm:block text-right">
              <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Status</span>
            </div>
            <div className="col-span-8 sm:col-span-1 md:col-span-1 text-right">
              <span className="font-mono-label text-mono-label text-on-surface-variant uppercase">Action</span>
            </div>
          </div>

          <div className="flex flex-col w-full h-[600px] overflow-y-auto" style={{ scrollbarWidth: 'none' }}>
            {loading ? (
              <div className="flex flex-col items-center justify-center h-full gap-sm text-on-surface-variant">
                <span className="material-symbols-outlined text-[32px] text-primary animate-spin">sync</span>
                <p className="font-body-sm text-body-sm">Loading customers...</p>
              </div>
            ) : filteredCustomers.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full gap-sm text-on-surface-variant p-xl text-center">
                <span className="material-symbols-outlined text-[40px] opacity-40">person_search</span>
                <p className="font-headline-sm text-headline-sm text-on-surface">No customers found</p>
                <p className="font-body-sm text-body-sm">Try a different search term.</p>
              </div>
            ) : (
              visibleCustomers.map((customer) => {
                const isInactive = normalizeStatus(customer.status) === 'inactive';
                const isSelected = customer.customerId === selectedId;
                return (
                  <div
                    key={customer.customerId}
                    onClick={() => setSelectedId(customer.customerId)}
                    className={`grid grid-cols-12 gap-md px-lg py-md items-center group hover:bg-surface transition-colors cursor-pointer border-b border-surface-container/50 ${
                      isInactive ? 'bg-error-container/20' : ''
                    } ${isSelected ? 'bg-surface' : ''}`}
                  >
                    <div className="col-span-11 sm:col-span-4 flex items-center gap-md min-w-0">
                      {customer.avatarUrl ? (
                        <div className="w-10 h-10 rounded-full bg-surface-container text-on-surface flex items-center justify-center shrink-0 overflow-hidden relative">
                          <img
                            className="w-full h-full object-cover mix-blend-multiply opacity-80"
                            alt=""
                            src={customer.avatarUrl}
                          />
                        </div>
                      ) : (
                        <div
                          className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 font-headline-sm text-headline-sm relative ${
                            isSelected
                              ? 'bg-primary-container text-on-primary-container'
                              : 'bg-surface-container text-on-surface'
                          }`}
                        >
                          {initialOf(customer.companyName)}
                          {isInactive && (
                            <span className="absolute top-0 right-0 w-3 h-3 bg-error rounded-full border-2 border-surface-container-lowest" />
                          )}
                        </div>
                      )}
                      <div className="flex flex-col min-w-0">
                        <span className="font-headline-sm text-headline-sm text-on-surface truncate">
                          {customer.companyName}
                        </span>
                        <span className="font-mono-label text-mono-label text-outline truncate">
                          ID: {customer.customerCode || `CUST-${customer.customerId}`}
                        </span>
                      </div>
                    </div>

                    <div className="col-span-2 hidden sm:flex items-center gap-sm">
                      <span className="text-xl leading-none" title={customer.country || ''}>
                        {countryLabel(customer.country)}
                      </span>
                      <span className="font-body-sm text-body-sm text-on-surface-variant">
                        {customer.region || '-'}
                      </span>
                    </div>

                    <div className="col-span-3 hidden md:flex items-center gap-sm">
  <div className="flex flex-col">
    {orderVolumeByCustomer[customer.customerId] != null ? (
      <>
        <span className="font-headline-sm text-headline-sm text-on-surface">
          {orderVolumeByCustomer[customer.customerId].toLocaleString(undefined, {
            maximumFractionDigits: 2,
          })}{' '}
          kg
        </span>
        <div className="flex items-center gap-xs text-primary">
          <span className="material-symbols-outlined text-[16px]">
            inventory_2
          </span>
          <span className="font-label-md text-label-md">
            YTD volume
          </span>
        </div>
      </>
    ) : (
      <>
        <span className="font-headline-sm text-headline-sm text-on-surface opacity-50">
          0 kg
        </span>
        <div className="flex items-center gap-xs text-outline">
          <span className="font-label-md text-label-md">
            No orders this year
          </span>
        </div>
      </>
    )}
  </div>
</div>

                    <div className="col-span-2 hidden sm:flex justify-end">
                      <StatusBadge status={customer.status} />
                    </div>

                    <div className="col-span-1 flex justify-end relative">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOpenMenuCustomerId(openMenuCustomerId === customer.customerId ? null : customer.customerId);
                        }}
                        className="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container hover:text-primary transition-colors"
                      >
                        <span className="material-symbols-outlined text-[20px]">more_vert</span>
                      </button>
                      {openMenuCustomerId === customer.customerId && (
                        <div
                          onClick={(e) => e.stopPropagation()}
                          className="absolute right-0 top-full mt-xs bg-surface-container-lowest rounded-lg shadow-lg border border-outline-variant/30 py-xs z-30 min-w-[180px]"
                        >
                          <button
                            onClick={() => {
                              setOpenMenuCustomerId(null);
                              setSelectedId(customer.customerId);
                              openEditCustomer(customer);
                            }}
                            className="w-full px-md py-sm text-left font-body-sm hover:bg-surface-container text-on-surface flex items-center gap-xs"
                          >
                            <span className="material-symbols-outlined text-[18px] text-on-surface-variant">edit</span>
                            Edit Customer
                          </button>
                          <button
                            onClick={() => {
                              setOpenMenuCustomerId(null);
                              setSelectedId(customer.customerId);
                              openCreateContact();
                            }}
                            className="w-full px-md py-sm text-left font-body-sm hover:bg-surface-container text-on-surface flex items-center gap-xs"
                          >
                            <span className="material-symbols-outlined text-[18px] text-on-surface-variant">person_add</span>
                            Add Contact
                          </button>
                          <button
                            onClick={() => {
                              setOpenMenuCustomerId(null);
                              setPendingDeleteCustomer(customer);
                            }}
                            className="w-full px-md py-sm text-left font-body-sm hover:bg-error-container/30 text-error flex items-center gap-xs"
                          >
                            <span className="material-symbols-outlined text-[18px]">delete</span>
                            Delete Customer
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>

          <div className="bg-surface-container-lowest px-lg py-md flex items-center justify-between border-t border-surface-container/50 mt-auto">
            <span className="font-body-sm text-body-sm text-on-surface-variant">
              Showing {visibleCustomers.length} of {filteredCustomers.length} matching ({totalCount} total)
            </span>
            <div className="flex items-center gap-sm">
              <button
                type="button"
                onClick={() => setCustomerPage((page) => Math.max(1, page - 1))}
                className="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-50"
                disabled={customerPage === 1}
                title="Previous customer page"
              >
                <span className="material-symbols-outlined text-[20px]">chevron_left</span>
              </button>
              <span className="font-mono-label text-mono-label text-on-surface-variant">
                {customerPage} / {customerPageCount}
              </span>
              <button
                type="button"
                onClick={() => setCustomerPage((page) => Math.min(customerPageCount, page + 1))}
                disabled={customerPage === customerPageCount}
                title="Next customer page"
                className="w-8 h-8 rounded-full flex items-center justify-center text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[20px]">chevron_right</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right context panel */}
        <div className="col-span-12 lg:col-span-4 flex flex-col gap-gutter">
          <div className="bg-surface-container-lowest rounded-xl shadow-md p-lg flex flex-col relative overflow-hidden h-auto min-h-[400px]">
            <div className="absolute right-0 top-0 w-48 h-48 bg-primary/5 rounded-bl-full pointer-events-none -z-0" />
            {selectedCustomer ? (
              <>
                <div className="flex items-start justify-between relative z-10 mb-xl">
                  <div className="w-16 h-16 rounded-2xl bg-primary text-on-primary flex items-center justify-center font-display-lg text-display-lg shadow-sm">
                    {initialOf(selectedCustomer.companyName)}
                  </div>
                  <button
                    onClick={() => openEditCustomer(selectedCustomer)}
                    className="text-primary hover:bg-primary/10 px-md py-xs rounded-full font-label-md text-label-md transition-colors flex items-center gap-xs"
                  >
                    <span className="material-symbols-outlined text-[16px]">edit</span>
                    EDIT
                  </button>
                </div>
                <div className="relative z-10 mb-lg">
                  <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
                    {selectedCustomer.companyName}
                  </h3>
                  <p className="font-body-md text-body-md text-on-surface-variant flex items-center gap-xs">
                    <span className="material-symbols-outlined text-[16px]">location_on</span>
                    {locationLine || 'Location unavailable'}
                  </p>
                  <div className="flex flex-wrap items-center gap-xs mt-md">
                    <StatusBadge status={selectedCustomer.status} />
                    <span className="px-sm py-xs bg-surface-container rounded-full font-label-md text-label-md text-on-surface-variant">
                      {selectedCustomer.customerType || 'customer'}
                    </span>
                    {selectedCustomer.paymentTerms && (
                      <span className="px-sm py-xs bg-surface-container rounded-full font-label-md text-label-md text-on-surface-variant">
                        {selectedCustomer.paymentTerms}
                      </span>
                    )}
                    <span className="font-mono-label text-mono-label text-outline">
                      {selectedCustomer.customerCode || `#${selectedCustomer.customerId}`}
                    </span>
                  </div>
                </div>

                {/* Certifications */}
                {certifications.length > 0 && (
                  <div className="relative z-10 mb-md">
                    <div className="flex items-center justify-between mb-sm">
                      <span className="font-mono-label text-mono-label text-on-surface-variant uppercase text-xs tracking-wider">
                        Certifications
                      </span>
                      <button
                        onClick={() => fetchCustomerCertifications(selectedCustomer.customerId)}
                        className="text-primary font-label-md text-label-xs hover:underline"
                        title="Refresh certifications"
                      >
                        <span className="material-symbols-outlined text-[16px]">refresh</span>
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-xs">
                      {certifications.map((cert) => {
                        const linked = selectedCustomerCerts.some(
                          (cc) => cc.certificationId === cert.certificationId
                        );
                        return (
                          <button
                            key={cert.certificationId}
                            onClick={() => toggleCertification(cert.certificationId)}
                            className={`px-sm py-xs rounded-full font-label-md text-label-xs transition-colors ${
                              linked
                                ? 'bg-primary/15 text-primary border border-primary/30'
                                : 'bg-surface-container text-on-surface-variant border border-outline-variant/30 hover:bg-surface-container-high'
                            }`}
                          >
                            {linked && (
                              <span className="material-symbols-outlined text-[14px] align-text-bottom mr-xs">
                                check
                              </span>
                            )}
                            {cert.name}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                <div className="relative z-10 mt-auto">
                  <div className="flex items-center justify-between mb-sm">
                    <span className="font-mono-label text-mono-label text-on-surface-variant uppercase text-xs tracking-wider">
                      Contacts ({selectedContacts.length})
                    </span>
                    <button
                      onClick={openCreateContact}
                      className="text-primary font-label-md text-label-xs hover:underline flex items-center gap-xs"
                    >
                      <span className="material-symbols-outlined text-[16px]">person_add</span>
                      ADD
                    </button>
                  </div>
                  <div className="flex flex-col gap-md">
                    {selectedContacts.length === 0 ? (
                      <p className="font-body-sm text-body-sm text-on-surface-variant italic">
                        No contacts on file.
                      </p>
                    ) : (
                      selectedContacts.map((contact) => (
                        <div
                          key={contact.contactId}
                          className="bg-surface-container-low/50 rounded-lg p-sm border border-surface-container"
                        >
                          <div className="flex items-start justify-between mb-xs">
                            <div className="flex items-center gap-sm">
                              <div className="w-8 h-8 rounded-full bg-surface-container text-on-surface-variant flex items-center justify-center font-label-md">
                                {initialOf(contact.fullName)}
                              </div>
                              <div>
                                <span className="font-body-sm text-body-sm text-on-surface flex items-center gap-xs">
                                  {contact.fullName}
                                  {contact.isPrimary && (
                                    <span className="px-xs py-unit bg-primary/15 text-primary text-[10px] rounded-full font-label-md">
                                      Primary
                                    </span>
                                  )}
                                </span>
                                <span className="font-label-md text-label-md text-primary uppercase">
                                  {contact.role || 'Contact'}
                                </span>
                              </div>
                            </div>
                            <div className="flex items-center gap-xs">
                              <button
                                onClick={() => openEditContact(contact)}
                                className="p-xs text-on-surface-variant hover:text-primary transition-colors"
                                title="Edit contact"
                              >
                                <span className="material-symbols-outlined text-[16px]">edit</span>
                              </button>
                              <button
                                onClick={() => setPendingDeleteContact(contact)}
                                className="p-xs text-on-surface-variant hover:text-error transition-colors"
                                title="Delete contact"
                              >
                                <span className="material-symbols-outlined text-[16px]">delete</span>
                              </button>
                            </div>
                          </div>
                          {contact.email && (
                            <div className="flex items-center gap-xs mt-xs">
                              <span className="material-symbols-outlined text-on-surface-variant text-[14px]">mail</span>
                              <a
                                href={`mailto:${contact.email}`}
                                className="font-body-sm text-body-sm text-primary hover:underline truncate"
                              >
                                {contact.email}
                              </a>
                            </div>
                          )}
                          {contact.phone && (
                            <div className="flex items-center gap-xs mt-xs">
                              <span className="material-symbols-outlined text-on-surface-variant text-[14px]">call</span>
                              <a
                                href={`tel:${contact.phone}`}
                                className="font-mono-label text-mono-label text-on-surface truncate"
                              >
                                {contact.phone}
                              </a>
                            </div>
                          )}
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-on-surface-variant relative z-10">
                <span className="material-symbols-outlined text-[40px] opacity-40 mb-sm">person</span>
                <p className="font-body-sm text-body-sm">Select a customer to view details</p>
              </div>
            )}
          </div>

          <div className="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden flex flex-col h-[280px]">
            <div className="px-lg py-md flex items-center justify-between border-b border-surface-container/50">
              <span className="font-headline-sm text-headline-sm text-on-surface">Logistics Hub</span>
              <span className="material-symbols-outlined text-on-surface-variant">local_shipping</span>
            </div>
            <div
              className="w-full flex-1 bg-surface-variant relative"
              data-location={locationLine || 'Munich, Germany'}
            >
              <div
                className="absolute inset-0 bg-cover bg-center mix-blend-luminosity opacity-80"
                style={{ backgroundImage: MAP_IMAGE }}
              />
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
                <div className="w-12 h-12 bg-primary/20 rounded-full flex items-center justify-center animate-pulse">
                  <div className="w-4 h-4 bg-primary rounded-full shadow-lg" />
                </div>
              </div>
              {locationLine && (
                <div className="absolute bottom-md left-md bg-surface-container-lowest px-md py-sm rounded-lg shadow-md">
                  <p className="font-label-md text-label-md text-on-surface">{locationLine}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Create / Edit Customer Modal */}
      {showCustomerModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-2xl p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowCustomerModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingCustomerId != null ? `Edit Customer #${editingCustomerId}` : 'Add New Customer'}
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {editingCustomerId != null ? (
                <>Updates via <code className="text-primary">PUT /api/customers/{editingCustomerId}</code></>
              ) : (
                <>Creates via <code className="text-primary">POST /api/customers</code></>
              )}
            </p>
            <form onSubmit={handleSaveCustomer} className="flex flex-col gap-md">
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Company Name *
                </label>
                <input
                  required
                  type="text"
                  value={customerForm.companyName}
                  onChange={(e) => setCustomerForm((f) => ({ ...f, companyName: e.target.value }))}
                  placeholder="e.g. AgriTech Solutions GmbH"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Customer Code
                  </label>
                  <input
                    type="text"
                    value={customerForm.customerCode}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, customerCode: e.target.value }))}
                    placeholder="e.g. CUST-0001-GER (auto-generated if blank)"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Customer Type
                  </label>
                  <select
                    value={customerForm.customerType}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, customerType: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    {CUSTOMER_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t.replace('_', ' ').replace(/\b\w/g, (l) => l.toUpperCase())}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Country
                  </label>
                  <input
                    type="text"
                    value={customerForm.country}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, country: e.target.value }))}
                    placeholder="e.g. Germany"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Region / State
                  </label>
                  <input
                    type="text"
                    value={customerForm.region}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, region: e.target.value }))}
                    placeholder="e.g. Bavaria"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    City
                  </label>
                  <input
                    type="text"
                    value={customerForm.city}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, city: e.target.value }))}
                    placeholder="e.g. Munich"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Status
                  </label>
                  <select
                    value={customerForm.status}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, status: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>
                        {s.charAt(0).toUpperCase() + s.slice(1)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                    Payment Terms
                  </label>
                  <select
                    value={customerForm.paymentTerms}
                    onChange={(e) => setCustomerForm((f) => ({ ...f, paymentTerms: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    <option value="">Select...</option>
                    {PAYMENT_TERMS.map((p) => (
                      <option key={p} value={p}>{p}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowCustomerModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface hover:bg-surface-container-high transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingCustomer}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs hover:bg-on-primary-fixed-variant transition-colors"
                >
                  {savingCustomer ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Saving...
                    </>
                  ) : editingCustomerId != null ? (
                    <>
                      <span className="material-symbols-outlined text-[18px]">save</span> Save Changes
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Create Customer
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create / Edit Contact Modal */}
      {showContactModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-lg p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowContactModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingContactId != null ? 'Edit Contact' : 'Add Contact'}
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {editingContactId != null ? (
                <>Updates via <code className="text-primary">PUT /api/customer-contacts/{editingContactId}</code></>
              ) : (
                <>Creates via <code className="text-primary">POST /api/customer-contacts</code></>
              )}
            </p>
            <form onSubmit={handleSaveContact} className="flex flex-col gap-md">
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Full Name *
                </label>
                <input
                  required
                  type="text"
                  value={contactForm.fullName}
                  onChange={(e) => setContactForm((f) => ({ ...f, fullName: e.target.value }))}
                  placeholder="e.g. Hans Weber"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Role / Title
                </label>
                <input
                  type="text"
                  value={contactForm.role}
                  onChange={(e) => setContactForm((f) => ({ ...f, role: e.target.value }))}
                  placeholder="e.g. Procurement Lead"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Email
                </label>
                <input
                  type="email"
                  value={contactForm.email}
                  onChange={(e) => setContactForm((f) => ({ ...f, email: e.target.value }))}
                  placeholder="name@company.de"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>
              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">
                  Phone
                </label>
                <input
                  type="tel"
                  value={contactForm.phone}
                  onChange={(e) => setContactForm((f) => ({ ...f, phone: e.target.value }))}
                  placeholder="+49 89 1234 5678"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>
              <label className="flex items-center gap-sm cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={contactForm.isPrimary}
                  onChange={(e) => setContactForm((f) => ({ ...f, isPrimary: e.target.checked }))}
                  className="w-5 h-5 rounded accent-primary"
                />
                <span className="font-label-md text-label-md">Mark as Primary Contact</span>
              </label>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowContactModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface hover:bg-surface-container-high transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingContact}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs hover:bg-on-primary-fixed-variant transition-colors"
                >
                  {savingContact ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Saving...
                    </>
                  ) : editingContactId != null ? (
                    <>
                      <span className="material-symbols-outlined text-[18px]">save</span> Save Changes
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Add Contact
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete customer confirmation */}
      {pendingDeleteCustomer && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete customer?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/customers/{pendingDeleteCustomer.customerId}</code> for{' '}
              <strong>{pendingDeleteCustomer.companyName}</strong>. This cannot be undone.
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg line-clamp-3">
              {[pendingDeleteCustomer.city, pendingDeleteCustomer.region, pendingDeleteCustomer.country]
                .filter(Boolean)
                .join(', ') || 'No location set'}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteCustomer(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingCustomer}
                onClick={confirmDeleteCustomer}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60 hover:opacity-90 transition-opacity"
              >
                {deletingCustomer ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete contact confirmation */}
      {pendingDeleteContact && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete contact?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/customer-contacts/{pendingDeleteContact.contactId}</code> for{' '}
              <strong>{pendingDeleteContact.fullName}</strong>. This cannot be undone.
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {pendingDeleteContact.email || pendingDeleteContact.role || 'No additional details'}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteContact(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingContact}
                onClick={confirmDeleteContact}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60 hover:opacity-90 transition-opacity"
              >
                {deletingContact ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
