import { LEVEL_META, LEVEL_ORDER } from "../api/sitesecurityTypes";
import { isValidEmail } from "./credentialForm";

export const LEGACY_PERMISSION = "legacy_read_all";

export const CATEGORIES = [
    { value: "worker", label: "Worker" },
    { value: "visitor", label: "Visitor" },
    { value: "contractor", label: "Contractor" },
    { value: "security", label: "Security" },
];

export const ROLES = [
    { value: "user", label: "User" },
    { value: "admin", label: "Admin" },
];

/* legacy_read_all is never offered: it only exists on migrated accounts. */
export const PERMISSIONS = [
    { value: "view_dashboard", label: "Dashboard" },
    { value: "view_zones", label: "Perimeter & Zones" },
    { value: "view_access_log", label: "Access Log" },
    { value: "view_access_register", label: "Access Control (read-only)" },
];

export const ZONES = LEVEL_ORDER.map((id) => ({ value: id, label: LEVEL_META[id].label }));

const NAME_MAX = 100;
const PERMISSION_VALUES = PERMISSIONS.map((p) => p.value);
const ZONE_VALUES = ZONES.map((z) => z.value);
const CATEGORY_VALUES = CATEGORIES.map((c) => c.value);
const ROLE_VALUES = ROLES.map((r) => r.value);

export function emptyInviteForm() {
    return {
        fullName: "",
        email: "",
        emailConfirmation: "",
        category: "worker",
        role: "user",
        permissions: [],
        zoneIds: [],
        validUntil: "",
    };
}

export function toggleValue(list, value) {
    return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

function sameEmail(a, b) {
    return a.trim().toLowerCase() === b.trim().toLowerCase();
}

/* Shared by the invite and the edit form. `today` is a local yyyy-MM-dd date. */
function accessErrors(form, today) {
    const errors = {};
    const name = form.fullName.trim();
    if (!name) errors.fullName = "Enter the full name.";
    else if (name.length > NAME_MAX) errors.fullName = `At most ${NAME_MAX} characters.`;
    if (!CATEGORY_VALUES.includes(form.category)) errors.category = "Choose a category.";
    if (!ROLE_VALUES.includes(form.role)) errors.role = "Choose a role.";
    if (form.validUntil && !/^\d{4}-\d{2}-\d{2}$/.test(form.validUntil)) errors.validUntil = "Enter a valid date.";
    else if (form.validUntil && today && form.validUntil < today) errors.validUntil = "The date cannot be in the past.";
    return errors;
}

/** Field errors of the invite form, keyed like the API body; empty when it can be sent. */
export function inviteFormErrors(form, today) {
    const errors = accessErrors(form, today);
    if (!isValidEmail(form.email)) errors.email = "Enter a valid email address.";
    if (!form.emailConfirmation.trim()) errors.emailConfirmation = "Type the email again.";
    else if (!sameEmail(form.email, form.emailConfirmation)) errors.emailConfirmation = "The two emails do not match.";
    return errors;
}

export function isFormValid(errors) {
    return Object.keys(errors).length === 0;
}

/* Admins can do everything: their permission list stays empty. */
function accessPayload(form) {
    return {
        fullName: form.fullName.trim(),
        role: form.role,
        category: form.category,
        permissions: form.role === "admin" ? [] : form.permissions.filter((p) => PERMISSION_VALUES.includes(p)),
        zoneIds: form.zoneIds.filter((z) => ZONE_VALUES.includes(z)),
        validUntil: form.validUntil || null,
    };
}

export function buildInvitePayload(form) {
    return {
        ...accessPayload(form),
        email: form.email.trim(),
        emailConfirmation: form.emailConfirmation.trim(),
    };
}

export function editFormFromUser(user) {
    const permissions = Array.isArray(user.permissions) ? user.permissions : [];
    return {
        fullName: user.fullName ?? "",
        role: user.role ?? "user",
        category: user.category ?? "worker",
        permissions: permissions.filter((p) => PERMISSION_VALUES.includes(p)),
        zoneIds: Array.isArray(user.zoneIds) ? user.zoneIds : [],
        validUntil: user.validUntil ?? "",
        hadLegacyAccess: permissions.includes(LEGACY_PERMISSION),
        keepLegacyAccess: permissions.includes(LEGACY_PERMISSION),
    };
}

export function editFormErrors(form, today) {
    return accessErrors(form, today);
}

/* A migrated account keeps legacy_read_all unless the admin unticks it; it is never added. */
export function buildEditPayload(form) {
    const payload = accessPayload(form);
    if (form.role !== "admin" && form.hadLegacyAccess && form.keepLegacyAccess) {
        payload.permissions = [...payload.permissions, LEGACY_PERMISSION];
    }
    return payload;
}

const SHORT_PERMISSION = {
    view_dashboard: "Dashboard",
    view_zones: "Zones",
    view_access_log: "Log",
    view_access_register: "Access (read)",
    [LEGACY_PERMISSION]: "Legacy read-all",
};

/** Short text for the table: "Dashboard, Log" or "All (admin)". */
export function permissionsSummary(user) {
    if (user?.role === "admin") return "All (admin)";
    const list = (user?.permissions ?? []).map((p) => SHORT_PERMISSION[p] ?? p);
    return list.length ? list.join(", ") : "None";
}

export function zonesSummary(user) {
    const ids = user?.zoneIds ?? [];
    if (ids.length === 0) return "No zone";
    return ids.map((id) => ZONES.find((z) => z.value === id)?.label ?? id).join(", ");
}
