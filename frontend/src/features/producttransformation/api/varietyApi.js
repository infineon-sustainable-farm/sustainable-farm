/**
 * Product Transformation - variety catalogue (read only)
 *
 * The varieties belong to the plants module (app/ownerLinks.js, OWNER_LINKS.variety): product
 * transformation only picks one, by its key, from the catalogue plants serves.
 */

import { apiClient } from '../../../shared/api/client';

export const varietyApi = {
  /** Every variety of the plants catalogue, by name: `{ varietyKey, varietyName, ... }`. */
  getVarieties: () => apiClient.get('/api/plants/variety-references'),
};
