import PlantsLayout from "./components/PlantsLayout";
import VarietiesPage from "./components/VarietiesPage";
import GrowthCalendarPage from "./components/GrowthCalendarPage";

export default [
  {
    path: "plants",
    element: <PlantsLayout />,
    children: [
      { index: true, element: <VarietiesPage /> },
      { path: "growth-calendar", element: <GrowthCalendarPage /> },
    ],
  },
];
