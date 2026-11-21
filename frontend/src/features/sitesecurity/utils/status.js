/*
 * The server sends statuses, credential types and gate actions as fixed English values
 * ("Approved", "Visitor", "Entry"). The screens translate them with these keys of the
 * sitesecurity namespace and colour them with a tone of the shared kit; an unknown value is
 * shown as it is, in neutral.
 */
const STATUS_TONE = {
  Approved: "success",
  Active: "success",
  Online: "success",
  Secure: "success",
  "Open Access": "success",
  Authorized: "info",
  Monitored: "info",
  Protected: "info",
  Pending: "warning",
  Locked: "warning",
  "Locked Down": "warning",
  Denied: "error",
  Revoked: "error",
  Offline: "error",
  Expired: "neutral",
  Unknown: "neutral",
};

/* Server value to translation key: "Open Access" becomes "openAccess". */
export function enumKey(value) {
  return String(value ?? "")
    .trim()
    .replace(/[^A-Za-z0-9]+(.)?/g, (_, next) => (next ? next.toUpperCase() : ""))
    .replace(/^./, (first) => first.toLowerCase());
}

export function statusTone(status) {
  return STATUS_TONE[status] ?? "neutral";
}

/* t is the sitesecurity translator; the raw value is the fallback for a value the UI does not know. */
export function translateEnum(t, group, value) {
  if (value === null || value === undefined || value === "") return "";
  return t(`enums.${group}.${enumKey(value)}`, { defaultValue: String(value) });
}
