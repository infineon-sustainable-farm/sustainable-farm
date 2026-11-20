import { useCallback, useEffect, useState } from 'react';
import { siteApi } from '../api/siteApi';

/**
 * Farms and coded fields (blocks) of the site referential, loaded from the farm module for the
 * farm and block pickers. Only the fields that carry a block code can be picked: Product
 * Transformation names a block by its code.
 */
export function useFarmSite() {
  const [farms, setFarms] = useState([]);
  const [fields, setFields] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const [farmData, fieldData] = await Promise.all([siteApi.getFarms(), siteApi.getFields()]);
      setFarms(Array.isArray(farmData) ? farmData : []);
      setFields(Array.isArray(fieldData) ? fieldData.filter((field) => field.code) : []);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  return { farms, fields, loading, error, reload: load };
}

/** Same comparison as the backend: trimmed, upper case. */
export const sameBlockCode = (a, b) =>
  String(a ?? '').trim().toUpperCase() === String(b ?? '').trim().toUpperCase();

/** Option label of a block: its code, with the field name when it says more. */
export const blockLabel = (field) =>
  field.name && field.name !== field.code ? `${field.code} · ${field.name}` : field.code;

/**
 * The farm id (string) of a saved row, read back from its block code, or from the farm name the
 * API returns when the block is not in the list; '' when neither is found.
 */
export function farmIdOf(farms, fields, farmText, blockCode) {
  const field = fields.find((candidate) => sameBlockCode(candidate.code, blockCode));
  if (field) return String(field.farmId);
  const text = String(farmText ?? '').trim().toLowerCase();
  const farm = text && farms.find((candidate) => String(candidate.name ?? '').trim().toLowerCase() === text);
  return farm ? String(farm.id) : '';
}
