/*
 * Site Security endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
const BASE = "/api/v1/sitesecurity";

export const SITESECURITY_ENDPOINTS = {
  LOGIN: `${BASE}/auth/login`,
  REQUEST_CODE: `${BASE}/auth/request-code`,
  VERIFY_CODE: `${BASE}/auth/verify`,
  LOGOUT: `${BASE}/auth/logout`,
  INSPECT_TOKEN: `${BASE}/auth/tokens/inspect`,
  CONFIRM_INVITATION: `${BASE}/auth/invitations/confirm`,
  ACTIVATE: `${BASE}/auth/activate`,
  FORGOT_PASSWORD: `${BASE}/auth/forgot-password`,
  RESET_PASSWORD: `${BASE}/auth/reset-password`,
  SESSION: `${BASE}/session`,
  ACCOUNT_PASSWORD: `${BASE}/account/password`,
  ACCOUNT_PASSWORD_SETUP: `${BASE}/account/password/setup`,
  OVERVIEW: `${BASE}/overview`,
  ZONES: `${BASE}/zones`,
  CREDENTIALS: `${BASE}/credentials`,
  LOGS: `${BASE}/logs`,
  USERS: `${BASE}/users`,
  USERS_SUMMARY: `${BASE}/users/summary`,
  CODE_SIGN_IN_SETTINGS: `${BASE}/settings/code-sign-in`,
  EMAIL_DELIVERIES: `${BASE}/email-deliveries`,
};
