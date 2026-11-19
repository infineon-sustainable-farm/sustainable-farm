import {
  Bell,
  FileText,
  LayoutGrid,
  Megaphone,
  ShoppingCart,
  Tag,
  TrendingUp,
  Truck,
  Users,
} from 'lucide-react';
import { lazyScreen } from '../../shared/utils/lazyScreen';

// Screens load on first visit, one chunk each.
const CampaignsPage = lazyScreen(() => import('./pages/CampaignsPage'));
const CustomersPage = lazyScreen(() => import('./pages/CustomersPage'));
const DashboardPage = lazyScreen(() => import('./pages/DashboardPage'));
const DeliveryPage = lazyScreen(() => import('./pages/DeliveryPage'));
const DemandAlertsPage = lazyScreen(() => import('./pages/DemandAlertsPage'));
const DemandReportsPage = lazyScreen(() => import('./pages/DemandReportsPage'));
const ForecastingPage = lazyScreen(() => import('./pages/ForecastingPage'));
const PricingPage = lazyScreen(() => import('./pages/PricingPage'));
const SalesChannelsPage = lazyScreen(() => import('./pages/SalesChannelsPage'));

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below.
export default [
  {
    path: 'salesmarketing',
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'forecasting', element: <ForecastingPage /> },
      { path: 'demand-alerts', element: <DemandAlertsPage /> },
      { path: 'reports', element: <DemandReportsPage /> },
      { path: 'crm', element: <CustomersPage /> },
      { path: 'sales-channels', element: <SalesChannelsPage /> },
      { path: 'pricing', element: <PricingPage /> },
      { path: 'campaigns', element: <CampaignsPage /> },
      { path: 'delivery', element: <DeliveryPage /> },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Sales and marketing renders inside the shell, with the same entries,
 * order and targets as the module's former sidebar.
 */
export const nav = {
  id: 'salesmarketing',
  basePath: '/salesmarketing',
  icon: ShoppingCart,
  nameKey: 'salesmarketing:name',
  shell: true,
  items: [
    { to: '/salesmarketing', labelKey: 'salesmarketing:nav.dashboard', icon: LayoutGrid, end: true },
    { to: '/salesmarketing/forecasting', labelKey: 'salesmarketing:nav.forecasting', icon: TrendingUp },
    { to: '/salesmarketing/demand-alerts', labelKey: 'salesmarketing:nav.demandAlerts', icon: Bell },
    { to: '/salesmarketing/reports', labelKey: 'salesmarketing:nav.reports', icon: FileText },
    { to: '/salesmarketing/crm', labelKey: 'salesmarketing:nav.customers', icon: Users },
    { to: '/salesmarketing/sales-channels', labelKey: 'salesmarketing:nav.salesChannels', icon: ShoppingCart },
    { to: '/salesmarketing/pricing', labelKey: 'salesmarketing:nav.pricing', icon: Tag },
    { to: '/salesmarketing/campaigns', labelKey: 'salesmarketing:nav.campaigns', icon: Megaphone },
    { to: '/salesmarketing/delivery', labelKey: 'salesmarketing:nav.delivery', icon: Truck },
  ],
};
