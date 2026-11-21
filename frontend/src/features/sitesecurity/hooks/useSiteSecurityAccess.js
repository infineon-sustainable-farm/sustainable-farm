import { useOutletContext } from "react-router-dom";

/* The authorization given by SiteSecurityGate, for the screens of the module. */
export function useSiteSecurityAccess() {
  return useOutletContext() ?? { authorization: null, screens: [] };
}
