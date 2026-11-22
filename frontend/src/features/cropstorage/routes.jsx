import {
    Bell,
    Boxes,
    ClipboardCheck,
    GitCompareArrows,
    LayoutDashboard,
    MapPinned,
    Truck,
    Warehouse,
} from "lucide-react";
import { lazyScreen } from "../../shared/utils/lazyScreen";

// Screens load on first visit, one chunk each.
const OverviewPage = lazyScreen(() => import("./components/CropStoragePage"));
const ZonesPage = lazyScreen(() => import("./components/ZonesPage"));
const BatchesPage = lazyScreen(() => import("./components/BatchesPage"));
const MovementsPage = lazyScreen(() => import("./components/MovementsPage"));
const AlertsPage = lazyScreen(() => import("./components/AlertsPage"));
const QualityLossPage = lazyScreen(() => import("./components/QualityLossPage"));
const ScenarioAnalysisPage = lazyScreen(() => import("./components/ScenarioAnalysisPage"));

import { CROP_STORAGE_BASE } from "./paths";

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below.
export default [
    {
        path: "crop-storage",
        children: [
            { index: true, element: <OverviewPage /> },
            { path: "zones", element: <ZonesPage /> },
            { path: "batches", element: <BatchesPage /> },
            { path: "movements", element: <MovementsPage /> },
            { path: "alerts", element: <AlertsPage /> },
            { path: "quality", element: <QualityLossPage /> },
            { path: "scenario-analysis", element: <ScenarioAnalysisPage /> },
        ],
    },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx).
 */
export const nav = {
    id: "cropstorage",
    basePath: CROP_STORAGE_BASE,
    icon: Warehouse,
    nameKey: "cropstorage:name",
    shell: true,
    items: [
        { to: CROP_STORAGE_BASE, labelKey: "cropstorage:nav.overview", icon: LayoutDashboard, end: true },
        { to: `${CROP_STORAGE_BASE}/zones`, labelKey: "cropstorage:nav.zones", icon: MapPinned },
        { to: `${CROP_STORAGE_BASE}/batches`, labelKey: "cropstorage:nav.batches", icon: Boxes },
        { to: `${CROP_STORAGE_BASE}/movements`, labelKey: "cropstorage:nav.movements", icon: Truck },
        { to: `${CROP_STORAGE_BASE}/alerts`, labelKey: "cropstorage:nav.alerts", icon: Bell },
        { to: `${CROP_STORAGE_BASE}/quality`, labelKey: "cropstorage:nav.quality", icon: ClipboardCheck },
        {
            to: `${CROP_STORAGE_BASE}/scenario-analysis`,
            labelKey: "cropstorage:nav.scenarioAnalysis",
            icon: GitCompareArrows,
        },
    ],
};
