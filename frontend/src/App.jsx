import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from './features/salesmarketing/components/layout/MainLayout';

import DashboardPage from './features/salesmarketing/pages/DashboardPage';
import ForecastingPage from './features/salesmarketing/pages/ForecastingPage';
import DemandAlertsPage from './features/salesmarketing/pages/DemandAlertsPage';
import DemandReportsPage from './features/salesmarketing/pages/DemandReportsPage';
import CustomersPage from './features/salesmarketing/pages/CustomersPage';
import SalesChannelsPage from './features/salesmarketing/pages/SalesChannelsPage';
import PricingPage from './features/salesmarketing/pages/PricingPage';
import CampaignsPage from './features/salesmarketing/pages/CampaignsPage';
import DeliveryPage from './features/salesmarketing/pages/DeliveryPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />

          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="forecasting" element={<ForecastingPage />} />
          <Route path="demand-alerts" element={<DemandAlertsPage />} />
          <Route path="reports" element={<DemandReportsPage />} />
          <Route path="crm" element={<CustomersPage />} />
          <Route path="sales-channels" element={<SalesChannelsPage />} />
          <Route path="pricing" element={<PricingPage />} />
          <Route path="campaigns" element={<CampaignsPage />} />
          <Route path="delivery" element={<DeliveryPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
