import {
  CheckCircle,
  Cog,
  Factory,
  FileText,
  Flame,
  LayoutDashboard,
  Package,
  PackageCheck,
  Settings,
  Truck,
  Waves,
} from 'lucide-react';
import ProductTransformationLayout from './components/ProductTransformationLayout';
import Dashboard from './components/Dashboard';
import RawIntakeList from './components/RawIntakeList';
import BatchList from './components/BatchList';
import WashSortRecordList from './components/WashSortRecordList';
import DryingRunList from './components/DryingRunList';
import QcCheckpointList from './components/QcCheckpointList';
import ComplianceScreen from './components/ComplianceScreen';
import PackagingScreen from './components/PackagingScreen';
import EquipmentList from './components/EquipmentList';

/**
 * Product Transformation routes.
 *
 * The module follows the same shell as machinery, plants and watersupply:
 * `/product-transformation` is the module home, and each tab is a real URL
 * rendered inside ProductTransformationLayout. There is one sidebar per
 * module, owned by the layout — no second navigation inside the screens.
 *
 * Screen order follows the processing line: reception, batch, washing,
 * drying, quality control, compliance, packaging, then the equipment registry.
 */
export default [
  {
    path: 'product-transformation',
    element: <ProductTransformationLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'raw-intake', element: <RawIntakeList /> },
      { path: 'batches', element: <BatchList /> },
      { path: 'washing-sorting', element: <WashSortRecordList /> },
      { path: 'drying', element: <DryingRunList /> },
      { path: 'quality-control', element: <QcCheckpointList /> },
      { path: 'compliance', element: <ComplianceScreen /> },
      { path: 'packaging', element: <PackagingScreen /> },
      { path: 'equipment', element: <EquipmentList /> },
      {
        path: 'settings',
        element: (
          <div className="p-6">
            <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
            <p className="text-gray-600 mt-2">Settings module coming soon...</p>
          </div>
        ),
      },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Same entries, order and targets as the module's own sidebar.
 */
export const nav = {
  id: 'producttransformation',
  basePath: '/product-transformation',
  icon: Factory,
  nameKey: 'producttransformation:name',
  items: [
    { to: '/product-transformation', labelKey: 'producttransformation:nav.dashboard', icon: LayoutDashboard, end: true },
    { to: '/product-transformation/raw-intake', labelKey: 'producttransformation:nav.rawIntake', icon: Truck },
    { to: '/product-transformation/batches', labelKey: 'producttransformation:nav.batches', icon: Package },
    { to: '/product-transformation/washing-sorting', labelKey: 'producttransformation:nav.washingSorting', icon: Waves },
    { to: '/product-transformation/drying', labelKey: 'producttransformation:nav.drying', icon: Flame },
    { to: '/product-transformation/quality-control', labelKey: 'producttransformation:nav.qualityControl', icon: CheckCircle },
    { to: '/product-transformation/compliance', labelKey: 'producttransformation:nav.compliance', icon: FileText },
    { to: '/product-transformation/packaging', labelKey: 'producttransformation:nav.packaging', icon: PackageCheck },
    { to: '/product-transformation/equipment', labelKey: 'producttransformation:nav.equipment', icon: Cog },
    { to: '/product-transformation/settings', labelKey: 'producttransformation:nav.settings', icon: Settings },
  ],
};
