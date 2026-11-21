import { Fuel, Gauge, LayoutDashboard, Leaf, Settings, SlidersHorizontal, Sun, TriangleAlert, Zap } from "lucide-react";
import { lazyScreen } from "../../shared/utils/lazyScreen";

// Screens load on first visit, one chunk each.
const Dashboard = lazyScreen(() => import("./components/Dashboard"));
const EnergyComponents = lazyScreen(() => import("./components/EnergyComponents"));
const SolarGeneration = lazyScreen(() => import("./components/SolarGeneration"));
const ConsumptionTracking = lazyScreen(() => import("./components/ConsumptionTracking"));
const GeneratorBackup = lazyScreen(() => import("./components/GeneratorBackup"));
const CarbonFootprint = lazyScreen(() => import("./components/CarbonFootprint"));
const WhatIfAnalysis = lazyScreen(() => import("./components/WhatIfAnalysis"));
const AlertsNotifications = lazyScreen(() => import("./components/AlertsNotifications"));

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below. Each
// former tab of the module has its own URL; the dashboard, the tab opened first, is the index.
export default [
  {
    path: "energy-supply",
    children: [
      { index: true, element: <Dashboard /> },
      { path: "components", element: <EnergyComponents /> },
      { path: "solar", element: <SolarGeneration /> },
      { path: "consumption", element: <ConsumptionTracking /> },
      { path: "generator", element: <GeneratorBackup /> },
      { path: "carbon", element: <CarbonFootprint /> },
      { path: "scenario", element: <WhatIfAnalysis /> },
      { path: "alerts", element: <AlertsNotifications /> },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx), in the order of the former tabs.
 */
export const nav = {
  id: "energysupply",
  basePath: "/energy-supply",
  icon: Zap,
  nameKey: "energysupply:name",
  shell: true,
  items: [
    { to: "/energy-supply", labelKey: "energysupply:nav.dashboard", icon: LayoutDashboard, end: true },
    { to: "/energy-supply/components", labelKey: "energysupply:nav.components", icon: Settings },
    { to: "/energy-supply/solar", labelKey: "energysupply:nav.solar", icon: Sun },
    { to: "/energy-supply/consumption", labelKey: "energysupply:nav.consumption", icon: Gauge },
    { to: "/energy-supply/generator", labelKey: "energysupply:nav.generator", icon: Fuel },
    { to: "/energy-supply/carbon", labelKey: "energysupply:nav.carbon", icon: Leaf },
    { to: "/energy-supply/scenario", labelKey: "energysupply:nav.scenario", icon: SlidersHorizontal },
    { to: "/energy-supply/alerts", labelKey: "energysupply:nav.alerts", icon: TriangleAlert },
  ],
};
