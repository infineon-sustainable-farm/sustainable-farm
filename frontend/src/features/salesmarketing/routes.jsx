import MainLayout from './components/layout/MainLayout';
import CampaignsPage from './pages/CampaignsPage';
import CustomersPage from './pages/CustomersPage';
import DashboardPage from './pages/DashboardPage';
import DeliveryPage from './pages/DeliveryPage';
import DemandAlertsPage from './pages/DemandAlertsPage';
import DemandReportsPage from './pages/DemandReportsPage';
import ForecastingPage from './pages/ForecastingPage';
import PricingPage from './pages/PricingPage';
import SalesChannelsPage from './pages/SalesChannelsPage';

export default [
  {
    path: 'salesmarketing',
    element: <MainLayout />,
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
