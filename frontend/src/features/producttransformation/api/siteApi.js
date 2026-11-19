/**
 * Product Transformation - site referential (read only)
 * Farms and fields (blocks) are owned by the Water Supply "Farms & fields" screen (API under
 * /api/farm). Product Transformation only picks them: a raw intake, a batch or a harvest event
 * names an existing farm and the code of one of its blocks.
 */

import { apiClient } from '../../../shared/api/client';

export const siteApi = {
  /** Farms: [{ id, name, ... }] */
  getFarms: () => apiClient.get('/api/farm/farms'),

  /** Fields (blocks): [{ id, farmId, code, name, ... }]; `code` is null for a block without code. */
  getFields: () => apiClient.get('/api/farm/fields'),
};

export default siteApi;
