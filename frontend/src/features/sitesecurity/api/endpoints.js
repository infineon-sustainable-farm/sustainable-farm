/* Site Security API, under the module prefix of the platform (docs/adr/0002). */
const BASE = "/api/site-security";

export const SITESECURITY_ENDPOINTS = {
  OVERVIEW: `${BASE}/overview`,
  ZONES: `${BASE}/zones`,
  CREDENTIALS: `${BASE}/credentials`,
  LOGS: `${BASE}/logs`,
  AUTHORIZATION: `${BASE}/authorization/current`,
};
