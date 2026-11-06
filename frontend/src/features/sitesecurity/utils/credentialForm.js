import { LEVEL_ORDER } from "../api/sitesecurityTypes";

const VISITOR_LEVELS = ["open", "controlled"];
const DEFAULT_VALIDITY_DAYS = 30;
const FINAL_STATUSES = ["Revoked", "Expired"];
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+$/;

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

export function isValidEmail(email) {
    return EMAIL_SHAPE.test(email.trim());
}

export function isCredentialFormComplete({ name, email, role, validUntil }) {
    return Boolean(name.trim() && isValidEmail(email) && role.trim() && validUntil);
}

export function buildCredentialPayload({ name, email, role, type, level, validUntil }) {
    return { name: name.trim(), email: email.trim(), role: role.trim(), type, level, validUntil };
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

export function emailStatusLabel(user) {
    const subject = user.status === "Revoked" ? "Revocation email" : "Access email";
    switch (user.notificationStatus) {
        case "pending":
            return `${subject} sending...`;
        case "sent":
            return `${subject} sent`;
        case "failed":
            return `${subject} not sent`;
        default:
            return null;
    }
}
