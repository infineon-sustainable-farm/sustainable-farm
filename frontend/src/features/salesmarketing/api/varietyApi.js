import { apiClient } from '../../../shared/api/client';

/** The variety catalogue, owned by plants: the harvest months of each variety. */
export const varietyApi = {
  getVarietyReferences: () => apiClient.get('/api/plants/variety-references'),
};
