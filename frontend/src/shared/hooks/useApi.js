/**
 * Shared React hooks for API data fetching
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../api/apiClient';

/**
 * Custom hook for fetching data from API
 */
export const useApi = (endpoint, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiClient.get(endpoint, options);
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [endpoint, options]);

  useEffect(() => {
    if (endpoint) {
      fetchData();
    }
  }, [endpoint, fetchData]);

  return { data, loading, error, refetch: fetchData };
};

/**
 * Custom hook for POST requests
 */
export const useApiPost = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const postData = useCallback(async (endpoint, payload, options = {}) => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiClient.post(endpoint, payload, options);
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { postData, loading, error, data };
};

/**
 * Custom hook for PUT requests
 */
export const useApiPut = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const putData = useCallback(async (endpoint, payload, options = {}) => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiClient.put(endpoint, payload, options);
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { putData, loading, error, data };
};

/**
 * Custom hook for DELETE requests
 */
export const useApiDelete = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const deleteData = useCallback(async (endpoint, options = {}) => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiClient.delete(endpoint, options);
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { deleteData, loading, error, data };
};
