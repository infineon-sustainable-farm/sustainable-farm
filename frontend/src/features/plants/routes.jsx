import { Apple, Bug, CalendarDays, FlaskConical, Home, Leaf, Sprout, TrendingUp } from "lucide-react";
import { lazyScreen } from "../../shared/utils/lazyScreen";

// Screens load on first visit, one chunk each.
const OverviewPage = lazyScreen(() => import("./components/OverviewPage"));
const VarietiesPage = lazyScreen(() => import("./components/VarietiesPage"));
const GrowthCalendarPage = lazyScreen(() => import("./components/GrowthCalendarPage"));
const HarvestMaturityPage = lazyScreen(() => import("./components/HarvestMaturityPage"));
const FertilizerInventoryPage = lazyScreen(() => import("./components/FertilizerInventoryPage"));
const DiseasesAlertsPage = lazyScreen(() => import("./components/DiseasesAlertsPage"));
const NurseryPage = lazyScreen(() => import("./components/NurseryPage"));
const ScenarioAnalysisPage = lazyScreen(() => import("./components/ScenarioAnalysisPage"));

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below.
export default [
  {
    path: "plants",
    children: [
      { index: true, element: <OverviewPage /> },
      { path: "varieties", element: <VarietiesPage /> },
      { path: "growth-calendar", element: <GrowthCalendarPage /> },
      { path: "fertilizer-inventory", element: <FertilizerInventoryPage /> },
      { path: "diseases-alerts", element: <DiseasesAlertsPage /> },
      { path: "harvest-maturity", element: <HarvestMaturityPage /> },
      { path: "nursery", element: <NurseryPage /> },
      { path: "scenario-analysis", element: <ScenarioAnalysisPage /> },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Plants renders inside the shell, with the entries, order and targets
 * its own sidebar had.
 */
export const nav = {
  id: "plants",
  basePath: "/plants",
  icon: Sprout,
  nameKey: "plants:name",
  shell: true,
  items: [
    { to: "/plants", labelKey: "plants:nav.overview", icon: Home, end: true },
    { to: "/plants/varieties", labelKey: "plants:nav.varieties", icon: Sprout },
    { to: "/plants/growth-calendar", labelKey: "plants:nav.growthCalendar", icon: CalendarDays },
    { to: "/plants/fertilizer-inventory", labelKey: "plants:nav.fertilizerInventory", icon: FlaskConical },
    { to: "/plants/diseases-alerts", labelKey: "plants:nav.diseasesAlerts", icon: Bug },
    { to: "/plants/harvest-maturity", labelKey: "plants:nav.harvestMaturity", icon: Apple },
    { to: "/plants/nursery", labelKey: "plants:nav.nursery", icon: Leaf },
    { to: "/plants/scenario-analysis", labelKey: "plants:nav.scenarioAnalysis", icon: TrendingUp },
  ],
};
