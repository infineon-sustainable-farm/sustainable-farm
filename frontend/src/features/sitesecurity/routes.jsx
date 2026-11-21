import { DoorOpen, KeyRound, LayoutDashboard, ScrollText, ShieldCheck } from "lucide-react";
import { lazyScreen } from "../../shared/utils/lazyScreen";

// Screens load on first visit, one chunk each. They render inside the application shell
// (app/AppLayout.jsx, nav.shell below), under the authorization gate of the module.
const SiteSecurityGate = lazyScreen(() => import("./components/SiteSecurityGate"));
const Dashboard = lazyScreen(() => import("./screens/Dashboard"));
const AccessControl = lazyScreen(() => import("./screens/AccessControl"));
const ZonesScreen = lazyScreen(() => import("./screens/ZonesScreen"));
const AccessLogScreen = lazyScreen(() => import("./screens/AccessLogScreen"));

export default [
  {
    path: "sitesecurity",
    element: <SiteSecurityGate />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: "access", element: <AccessControl /> },
      { path: "zones", element: <ZonesScreen /> },
      { path: "log", element: <AccessLogScreen /> },
    ],
  },
];

/** Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher. */
export const nav = {
  id: "sitesecurity",
  basePath: "/sitesecurity",
  icon: ShieldCheck,
  nameKey: "sitesecurity:name",
  shell: true,
  items: [
    { to: "/sitesecurity", labelKey: "sitesecurity:nav.dashboard", icon: LayoutDashboard, end: true },
    { to: "/sitesecurity/access", labelKey: "sitesecurity:nav.access", icon: KeyRound },
    { to: "/sitesecurity/zones", labelKey: "sitesecurity:nav.zones", icon: DoorOpen },
    { to: "/sitesecurity/log", labelKey: "sitesecurity:nav.log", icon: ScrollText },
  ],
};
