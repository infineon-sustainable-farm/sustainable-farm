import {
  CheckCircle,
  Cog,
  Factory,
  FileText,
  Flame,
  LayoutDashboard,
  Package,
  PackageCheck,
  Truck,
  Waves,
} from 'lucide-react';
import { lazyScreen } from '../../shared/utils/lazyScreen';

// Screens load on first visit, one chunk each.
const Dashboard = lazyScreen(() => import('./components/Dashboard'));
const RawIntakeList = lazyScreen(() => import('./components/RawIntakeList'));
const BatchList = lazyScreen(() => import('./components/BatchList'));
const WashSortRecordList = lazyScreen(() => import('./components/WashSortRecordList'));
const DryingRunList = lazyScreen(() => import('./components/DryingRunList'));
const QcCheckpointList = lazyScreen(() => import('./components/QcCheckpointList'));
const ComplianceScreen = lazyScreen(() => import('./components/ComplianceScreen'));
const PackagingScreen = lazyScreen(() => import('./components/PackagingScreen'));
const EquipmentList = lazyScreen(() => import('./components/EquipmentList'));

/**
 * Product Transformation routes. The screens render inside the application shell
 * (app/AppLayout.jsx), see nav.shell below.
 *
 * Screen order follows the processing line: reception, batch, washing, drying, quality control,
 * compliance, packaging, then the equipment used on the line.
 */
export default [
  {
    path: 'product-transformation',
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
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx).
 */
export const nav = {
  id: 'producttransformation',
  basePath: '/product-transformation',
  icon: Factory,
  nameKey: 'producttransformation:name',
  shell: true,
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
  ],
};
