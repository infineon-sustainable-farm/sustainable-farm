/*
 * What the signed-in account may open. Admins see everything; a user sees
 * the screens their permissions name (legacy_read_all = the four views).
 * The server filters the data itself, this only shapes the navigation.
 */
export const OPERATION_SCREENS = ["dashboard", "access", "zones", "log"];

const SCREEN_PERMISSION = {
    dashboard: "view_dashboard",
    access: "view_access_register",
    zones: "view_zones",
    log: "view_access_log",
};

export function isAdmin(session) {
    return session?.role === "admin";
}

export function visibleScreens(session) {
    if (!session) return [];
    if (isAdmin(session)) return [...OPERATION_SCREENS, "users"];
    const permissions = session.permissions ?? [];
    if (permissions.includes("legacy_read_all")) return [...OPERATION_SCREENS];
    return OPERATION_SCREENS.filter((screen) => permissions.includes(SCREEN_PERMISSION[screen]));
}

export function canOpen(session, screen) {
    return visibleScreens(session).includes(screen);
}

export function accessLabel(session) {
    if (isAdmin(session)) return "Admin access";
    if (session?.permissions?.includes("legacy_read_all")) return "Legacy read-only access";
    return "User access";
}
