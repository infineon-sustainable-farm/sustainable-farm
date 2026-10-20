import CropStorageLayout from "./components/CropStorageLayout";
import CropStoragePage from "./components/CropStoragePage";

export default [
  {
    path: "cropstorage",
    element: <CropStorageLayout />,
    children: [{ index: true, element: <CropStoragePage /> }],
  },
];
