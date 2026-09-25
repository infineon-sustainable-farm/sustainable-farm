/*
 * Site Security endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
export const SITESECURITY_ENDPOINTS = {
  OVERVIEW: "/api/v1/sitesecurity/overview",
  ZONES: "/api/v1/sitesecurity/zones",
  CREDENTIALS: "/api/v1/sitesecurity/credentials",
  LOGS: "/api/v1/sitesecurity/logs",
};
