import { LEVEL_ORDER } from "../api/sitesecurityTypes";

const VISITOR_LEVELS = ["open", "controlled"];
const DEFAULT_VALIDITY_DAYS = 30;
const FINAL_STATUSES = ["Revoked", "Expired"];

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

export function isCredentialFormComplete({ name, role, validUntil }) {
    return Boolean(name.trim() && role.trim() && validUntil);
}

export function buildCredentialPayload({ name, role, type, level, validUntil }) {
    return { name: name.trim(), role: role.trim(), type, level, validUntil };
}

export function canRevoke(user) {
    return !FINAL_STATUSES.includes(user.status);
}
