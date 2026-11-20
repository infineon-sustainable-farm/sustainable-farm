import CropStorageLayout from "./components/CropStorageLayout";
import CropStoragePage from "./components/CropStoragePage";
import ZonesPage from "./components/ZonesPage";
import BatchesPage from "./components/BatchesPage";
import MovementsPage from "./components/MovementsPage";
import AlertsPage from "./components/AlertsPage";
import QualityLossPage from "./components/QualityLossPage";
import ScenarioAnalysisPage from "./components/ScenarioAnalysisPage";

export default [
  {
    path: "cropstorage",
    element: <CropStorageLayout />,
    children: [
      { index: true, element: <CropStoragePage /> },
      { path: "zones", element: <ZonesPage /> },
      { path: "batches", element: <BatchesPage /> },
      { path: "movements", element: <MovementsPage /> },
      { path: "alerts", element: <AlertsPage /> },
      { path: "quality", element: <QualityLossPage /> },
      { path: "scenario-analysis", element: <ScenarioAnalysisPage /> },
    ],
  },
];
