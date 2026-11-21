import { LEVEL_ORDER } from "../api/sitesecurityTypes";

const VISITOR_LEVELS = ["open", "controlled"];
const DEFAULT_VALIDITY_DAYS = 30;
const FINAL_STATUSES = ["Revoked", "Expired"];

export const CREDENTIAL_TYPES = ["Staff", "Visitor", "Service", "Security"];

/* Local date, not toISOString(): that one is in UTC and can return the wrong day. */
export function toIsoDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function defaultValidUntil(today) {
  const date = new Date(today);
  date.setDate(date.getDate() + DEFAULT_VALIDITY_DAYS);
  return toIsoDate(date);
}

export function allowedLevels(type) {
  return type === "Visitor" ? VISITOR_LEVELS : LEVEL_ORDER;
}

export function levelForType(type, level) {
  const levels = allowedLevels(type);
  return levels.includes(level) ? level : levels[levels.length - 1];
}

/* The holder is picked in the shared people directory: the form keeps its id, never its name. */
export function isCredentialFormComplete({ personId, role, validUntil }) {
  return Boolean(personId && role.trim() && validUntil);
}

export function buildCredentialPayload({ personId, role, type, level, validUntil }) {
  return { personId: Number(personId), role: role.trim(), type, level, validUntil };
}

export function canRevoke(user) {
  return !FINAL_STATUSES.includes(user.status);
}

export function hasPendingEmail(users) {
  return users.some((user) => user.notificationStatus === "pending");
}

/* Keeps the order on screen: a new credential stays at the top of the list. */
export function mergeCredentials(current, latest) {
  const latestById = new Map(latest.map((user) => [user.id, user]));
  return current.map((user) => latestById.get(user.id) ?? user);
}

/* Translation key (sitesecurity namespace) of the email status, or null when no email is known. */
export function emailStatusKey(user) {
  const subject = user.status === "Revoked" ? "revocation" : "grant";
  switch (user.notificationStatus) {
    case "pending":
    case "sent":
    case "failed":
      return `access.email.${subject}.${user.notificationStatus}`;
    default:
      return null;
  }
}
