export const USER_STATUS_LABEL = {
    pending: "Pending",
    activation_started: "Activation started (link opened)",
    active: "Active",
    expired: "Expired",
    revoked: "Revoked",
    disabled: "Disabled",
};

export function userStatusLabel(status) {
    return USER_STATUS_LABEL[status] ?? "Unknown";
}

const INVITATION_OPEN = ["pending", "activation_started", "expired"];

/**
 * Actions offered on a row, in display order. The server still refuses what
 * is not allowed (last admin, for example); this only hides useless buttons.
 */
export function userActions(user) {
    const status = user?.status;
    const actions = [];
    if (status === "pending" || status === "expired") actions.push("resend");
    if (status === "active" && user.legacyCodeSignIn) actions.push("setupLink");
    if (INVITATION_OPEN.includes(status)) actions.push("copyLink", "revoke");
    if (status !== "revoked") actions.push("edit");
    if (status === "disabled") actions.push("enable");
    else if (status !== "revoked") actions.push("disable");
    actions.push("history");
    if (canConfirmDelivery(user?.emailDelivery)) actions.push("confirmDelivery");
    return actions;
}

export const ACTION_LABEL = {
    resend: "Resend invitation",
    setupLink: "Send password setup link",
    copyLink: "Copy activation link",
    revoke: "Revoke invitation",
    edit: "Edit",
    enable: "Enable",
    disable: "Disable",
    history: "History",
    confirmDelivery: "Confirm real delivery",
};

const SUBMISSION_LABEL = {
    queued: "Queued",
    sending: "Sending…",
    retry_scheduled: "Retry scheduled",
    /* The provider accepted the message: that is not a delivery. */
    submitted: "Submitted to provider",
    failed: "Not sent (failed)",
    local_console: "Not sent (written to server console)",
    mail_disabled: "Not sent (email disabled)",
};

const DELIVERY_LABEL = {
    unknown: "Delivery not confirmed",
    confirmed_by_admin: "Delivery confirmed by admin",
};

const KIND_LABEL = {
    invitation: "Invitation email",
    password_reset: "Password reset email",
    password_setup: "Password setup email",
};

export function emailSubmissionLabel(submission) {
    return SUBMISSION_LABEL[submission] ?? "Unknown";
}

export function emailDeliveryLabel(delivery) {
    return DELIVERY_LABEL[delivery] ?? DELIVERY_LABEL.unknown;
}

export function emailKindLabel(kind) {
    return KIND_LABEL[kind] ?? "Email";
}

export function isSubmissionFailed(submission) {
    return ["failed", "local_console", "mail_disabled"].includes(submission);
}

/* Only an email the provider accepted can be attested by an admin; one never sent cannot. */
export function canConfirmDelivery(email) {
    return Boolean(email?.deliveryId) && email.submission === "submitted" && email.delivery !== "confirmed_by_admin";
}

const IN_FLIGHT = ["queued", "sending", "retry_scheduled"];

/* Emails go out after the API has answered, so their result arrives later. */
export function hasEmailInFlight(users) {
    return users.some((user) => IN_FLIGHT.includes(user.emailDelivery?.submission));
}

export function actorLabel(actor) {
    switch (actor?.kind) {
        case "admin":
            return actor.email ? `Admin ${actor.email}` : "An admin";
        case "self":
            return actor.email ? `${actor.email} (self)` : "The user";
        case "system":
            return "System";
        default:
            return "Unknown";
    }
}

/** "invitation_resent" → "Invitation resent". */
/* A manual attestation must never read like a provider delivery report. */
const HISTORY_LABEL = {
    email_delivery_confirmed: "Delivery attested by admin (manual)",
};

export function historyActionLabel(action) {
    if (HISTORY_LABEL[action]) return HISTORY_LABEL[action];
    const text = String(action ?? "").replace(/[_.]+/g, " ").trim().toLowerCase();
    return text ? text[0].toUpperCase() + text.slice(1) : "Event";
}

export function formatInstant(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit", hour12: false });
}

export function formatDate(value) {
    if (!value) return "—";
    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) return "—";
    return date.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

/** The ✓/✗ list of the "Legacy email-code sign-in" card. */
export function codeSignInConditions(settings) {
    const c = settings?.conditions ?? {};
    const required = c.requiredAdminsWithPasswordSignIn ?? 2;
    const admins = c.adminsWithPasswordSignIn ?? 0;
    return [
        { id: "delivery", ok: Boolean(c.realDeliveryVerified), label: "A real email delivery was confirmed by an admin and its link was used" },
        { id: "admins", ok: admins >= required, label: `At least ${required} admins sign in with a password (now ${admins})` },
        { id: "allAdmins", ok: Boolean(c.allActiveAdminsHavePassword), label: "Every active admin has a password" },
    ];
}

/* "Pending" includes the invitations whose link was already opened. */
export function summaryCards(summary) {
    const s = summary ?? {};
    return [
        { id: "pending", label: "Pending", value: (s.pending ?? 0) + (s.activationStarted ?? 0), sub: `${s.activationStarted ?? 0} with link opened`, tone: "bg-amber-50 text-amber-700" },
        { id: "active", label: "Active", value: s.active ?? 0, sub: "Account activated", tone: "bg-emerald-50 text-emerald-700" },
        { id: "expired", label: "Expired", value: s.expired ?? 0, sub: "Invitation link expired", tone: "bg-slate-100 text-slate-600" },
        { id: "closed", label: "Revoked / disabled", value: (s.revoked ?? 0) + (s.disabled ?? 0), sub: `${s.revoked ?? 0} revoked · ${s.disabled ?? 0} disabled`, tone: "bg-rose-50 text-rose-700" },
    ];
}
