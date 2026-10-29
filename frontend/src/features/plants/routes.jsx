import PlantsLayout from "./components/PlantsLayout";
import OverviewPage from "./components/OverviewPage";
import VarietiesPage from "./components/VarietiesPage";
import GrowthCalendarPage from "./components/GrowthCalendarPage";
import HarvestMaturityPage from "./components/HarvestMaturityPage";
import FertilizerInventoryPage from "./components/FertilizerInventoryPage";
import DiseasesAlertsPage from "./components/DiseasesAlertsPage";
import NurseryPage from "./components/NurseryPage";
import ScenarioAnalysisPage from "./components/ScenarioAnalysisPage";

export default [
  {
    path: "plants",
    element: <PlantsLayout />,
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
