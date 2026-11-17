import { LayoutDashboard, Zap } from "lucide-react";
import EnergyApp from "./components/EnergyApp";

export default [
  {
    path: "energy-supply",
    element: <EnergyApp />,
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). The screens are still tabs held in state, so the module has a single entry, its base path,
 * until each tab gets its own URL (T35).
 */
export const nav = {
  id: "energysupply",
  basePath: "/energy-supply",
  icon: Zap,
  nameKey: "energysupply:name",
  items: [{ to: "/energy-supply", labelKey: "energysupply:nav.dashboard", icon: LayoutDashboard, end: true }],
};
