import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api/api';
import { getErrorMessage } from './customerUtils';

/**
 * Customers, orders, contacts and certifications of the CRM screen, loaded together; the status
 * filter is sent to the API. A failure of the customers is an error, a failure of the other lists
 * a partial load.
 */
export function useCustomersData({ statusFilter, setSelectedId, flashError }) {
  const { t } = useTranslation('salesmarketing');
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [contacts, setContacts] = useState([]);
  const [certifications, setCertifications] = useState([]);
  const [customerCerts, setCustomerCerts] = useState({});

  const fetchCustomersData = useCallback(async () => {
    const requestFailed = t('customers.errors.requestFailed');
    try {
      const [customerRes, orderRes, contactRes, certRes] = await Promise.allSettled([
        statusFilter === 'all' ? api.getCustomers() : api.getCustomers(statusFilter),
        api.getOrders(),
        api.getCustomerContacts(),
        api.getCertifications(),
      ]);
      setApiError(null);

      const failures = [];
      const failure = (key, reason) =>
        failures.push(t(`customers.errors.${key}`, { message: getErrorMessage(reason, requestFailed) }));

      if (customerRes.status === 'fulfilled') {
        const list = Array.isArray(customerRes.value) ? customerRes.value : [];
        setCustomers(list);
        setSelectedId((current) => (current == null && list.length > 0 ? list[0].customerId : current));
      } else {
        setCustomers([]);
        failure('customersFailed', customerRes.reason);
      }
      if (orderRes.status === 'fulfilled') {
        setOrders(Array.isArray(orderRes.value) ? orderRes.value : []);
      } else {
        setOrders([]);
        failure('ordersFailed', orderRes.reason);
      }
      if (contactRes.status === 'fulfilled') {
        setContacts(Array.isArray(contactRes.value) ? contactRes.value : []);
      } else {
        setContacts([]);
        failure('contactsFailed', contactRes.reason);
      }
      if (certRes.status === 'fulfilled') {
        setCertifications(Array.isArray(certRes.value) ? certRes.value : []);
      } else {
        setCertifications([]);
        failure('certificationsFailed', certRes.reason);
      }

      if (failures.length > 0 && customerRes.status === 'rejected') {
        setApiError(t('customers.errors.loadFailed', { details: failures.join(' - ') }));
      } else if (failures.length > 0) {
        setApiError(t('customers.errors.partialLoad', { details: failures.join(' - ') }));
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('customers.errors.loadDataFailed')));
      setCustomers([]);
      setContacts([]);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, setSelectedId, t]);

  useEffect(() => {
    void Promise.resolve().then(fetchCustomersData);
  }, [fetchCustomersData]);

  const fetchCustomerCertifications = useCallback(
    async (customerId) => {
      try {
        const res = await api.getCustomerCertifications(customerId);
        setCustomerCerts((prev) => ({ ...prev, [customerId]: Array.isArray(res) ? res : [] }));
      } catch (err) {
        setCustomerCerts((prev) => ({ ...prev, [customerId]: [] }));
        flashError(getErrorMessage(err, t('customers.errors.certificationsLoadFailed')));
      }
    },
    [flashError, t]
  );

  return {
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
  };
}

/** Order volume in kg of each customer over the current calendar year. */
export function orderVolumeByCustomer(orders, currentYear) {
  return orders.reduce((acc, order) => {
    const customerId = Number(order.customerId);
    const orderDate = order.orderDate;
    if (!customerId || !orderDate) return acc;
    const date = new Date(orderDate);
    if (date.getFullYear() !== currentYear) return acc;
    const volumeKg = Number(order.totalVolumeKg ?? 0);
    if (!Number.isFinite(volumeKg)) return acc;
    acc[customerId] = (acc[customerId] || 0) + volumeKg;
    return acc;
  }, {});
}
