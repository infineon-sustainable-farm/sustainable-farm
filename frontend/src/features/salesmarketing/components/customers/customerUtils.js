import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

export const CUSTOMER_TYPES = ['distributor', 'retailer', 'direct_export', 'other'];
export const STATUS_OPTIONS = ['active', 'inactive', 'prospect'];
export const PAYMENT_TERMS = ['Net 15', 'Net 30', 'Net 45', 'Net 60', 'Due on Receipt', 'Advance Payment'];
export const CUSTOMER_PAGE_SIZE = 8;

// Translation keys of the payment terms, which the API stores as English text.
const PAYMENT_TERM_KEYS = {
  'Net 15': 'net15',
  'Net 30': 'net30',
  'Net 45': 'net45',
  'Net 60': 'net60',
  'Due on Receipt': 'dueOnReceipt',
  'Advance Payment': 'advancePayment',
};

export function getErrorMessage(err, fallback) {
  const data = err?.data;
  if (!data) return err?.message || (err?.status ? `${fallback} (${err.status})` : fallback);
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.fieldErrors && typeof data.fieldErrors === 'object') {
      const details = Object.values(data.fieldErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

export function initialOf(name = '') {
  return (name.trim()[0] || '?').toUpperCase();
}

export function normalizeStatus(status) {
  return (status || 'prospect').toLowerCase();
}

export function locationOf(customer) {
  return customer ? [customer.city, customer.region, customer.country].filter(Boolean).join(', ') : '';
}

/** Customers matching the search and status, sorted by name, and the requested page of them. */
export function filterCustomers(customers, { searchQuery, statusFilter, sortAscending, page }) {
  const q = searchQuery.trim().toLowerCase();
  const filteredCustomers = customers.filter((c) => {
    if (statusFilter !== 'all' && normalizeStatus(c.status) !== normalizeStatus(statusFilter)) return false;
    if (!q) return true;
    const haystack = [c.companyName, c.customerCode, c.region, c.city, c.country, c.status]
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
  const customerPageCount = Math.max(1, Math.ceil(sortedCustomers.length / CUSTOMER_PAGE_SIZE));
  const currentCustomerPage = Math.min(page, customerPageCount);
  const visibleCustomers = sortedCustomers.slice(
    (currentCustomerPage - 1) * CUSTOMER_PAGE_SIZE,
    currentCustomerPage * CUSTOMER_PAGE_SIZE
  );
  return { filteredCustomers, visibleCustomers, customerPageCount, currentCustomerPage };
}

export function emptyCustomerForm() {
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

export function emptyContactForm(customerId = null) {
  return { customerId, fullName: '', email: '', phone: '', role: '', isPrimary: false };
}

/** Translated labels of the customer values: country, type, status, payment terms. */
export function useCustomerLabels() {
  const { t } = useTranslation('salesmarketing');
  return useMemo(
    () => ({
      country(country) {
        if (!country) return '';
        const c = country.toLowerCase().trim();
        if (c.includes('germany') || c === 'de') return t('customers.countries.germany');
        if (c.includes('burkina')) return t('customers.countries.burkinaFaso');
        if (c.includes('france') || c === 'fr') return t('customers.countries.france');
        if (c.includes('netherlands') || c === 'nl') return t('customers.countries.netherlands');
        return country;
      },
      type(value) {
        if (!value) return t('customers.typeFallback');
        return t(`customers.types.${value}`, { defaultValue: value });
      },
      status(value) {
        return t(`customers.status.${normalizeStatus(value)}`, { defaultValue: value });
      },
      paymentTerms(value) {
        const key = PAYMENT_TERM_KEYS[value];
        return key ? t(`customers.paymentTermsOptions.${key}`) : value;
      },
    }),
    [t]
  );
}
