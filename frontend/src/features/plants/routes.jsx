import PlantsLayout from "./components/PlantsLayout";
import VarietiesPage from "./components/VarietiesPage";
import GrowthCalendarPage from "./components/GrowthCalendarPage";
import HarvestMaturityPage from "./components/HarvestMaturityPage";
import FertilizerInventoryPage from "./components/FertilizerInventoryPage";

export default [
  {
    path: "plants",
    element: <PlantsLayout />,
    children: [
      { index: true, element: <VarietiesPage /> },
      { path: "growth-calendar", element: <GrowthCalendarPage /> },
      { path: "fertilizer-inventory", element: <FertilizerInventoryPage /> },
      { path: "harvest-maturity", element: <HarvestMaturityPage /> },
    ],
  },
];
