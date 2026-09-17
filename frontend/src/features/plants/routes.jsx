import VarietiesPage from "./components/VarietiesPage";
import GrowthCalendarPage from "./components/GrowthCalendarPage";

export default [
  {
    path: "plants",
    element: <VarietiesPage />,
  },
  {
    path: "plants/growth-calendar",
    element: <GrowthCalendarPage />,
  },
];
