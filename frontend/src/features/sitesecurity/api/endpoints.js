/*
 * Site Security endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
const BASE = "/api/v1/sitesecurity";

export const SITESECURITY_ENDPOINTS = {
  SESSION: `${BASE}/session`,
  OVERVIEW: `${BASE}/overview`,
  ZONES: `${BASE}/zones`,
  CREDENTIALS: `${BASE}/credentials`,
  LOGS: `${BASE}/logs`,
  AUTHORIZATION: `${BASE}/authorization/current`,
};
