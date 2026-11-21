/*
 * What the caller's Site Security authorization may open. Admins see everything; a user sees
 * the screens its permissions name (legacy_read_all = the four views). The server filters the
 * data itself and refuses what is not allowed; this only shapes the screens.
 */
export const OPERATION_SCREENS = ["dashboard", "access", "zones", "log"];

const SCREEN_PERMISSION = {
  dashboard: "view_dashboard",
  access: "view_access_register",
  zones: "view_zones",
  log: "view_access_log",
};

export function isAdmin(authorization) {
  return authorization?.role === "admin";
}

export function visibleScreens(authorization) {
  if (!authorization) return [];
  if (isAdmin(authorization)) return [...OPERATION_SCREENS];
  const permissions = authorization.permissions ?? [];
  if (permissions.includes("legacy_read_all")) return [...OPERATION_SCREENS];
  return OPERATION_SCREENS.filter((screen) => permissions.includes(SCREEN_PERMISSION[screen]));
}

export function canOpen(authorization, screen) {
  return visibleScreens(authorization).includes(screen);
}

/* Translation key (sitesecurity namespace) of the caller's access, shown in the page headers. */
export function accessLabelKey(authorization) {
  if (isAdmin(authorization)) return "access.admin";
  if (authorization?.permissions?.includes("legacy_read_all")) return "access.legacyRead";
  return "access.user";
}
