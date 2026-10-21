import CropStorageLayout from "./components/CropStorageLayout";
import CropStoragePage from "./components/CropStoragePage";
import ZonesPage from "./components/ZonesPage";
import BatchesPage from "./components/BatchesPage";
import MovementsPage from "./components/MovementsPage";

export default [
  {
    path: "cropstorage",
    element: <CropStorageLayout />,
    children: [
      { index: true, element: <CropStoragePage /> },
      { path: "zones", element: <ZonesPage /> },
      { path: "batches", element: <BatchesPage /> },
      { path: "movements", element: <MovementsPage /> },
    ],
  },
];
