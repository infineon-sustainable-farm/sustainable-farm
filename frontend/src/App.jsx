import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ErrorBoundary from './shared/components/common/ErrorBoundary.jsx';
import Layout from './shared/components/layout/Layout.jsx';
import DashboardPage from './pages/Dashboard/DashboardPage.jsx';
import HarvestPage from './pages/Harvest/HarvestPage.jsx';
import RawIntakePage from './pages/RawIntake/RawIntakePage.jsx';
import BatchesPage from './pages/Batches/BatchesPage.jsx';
import BatchDetailPage from './pages/Batches/BatchDetailPage.jsx';
import DryingPage from './pages/Drying/DryingPage.jsx';
import WashingSortingPage from './pages/WashingSorting/WashingSortingPage.jsx';
import EquipmentPage from './pages/Equipment/EquipmentPage.jsx';
import OperatorsPage from './pages/Operators/OperatorsPage.jsx';
import './assets/design-system.css';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/harvest" element={<HarvestPage />} />
            <Route path="/raw-intake" element={<RawIntakePage />} />
            <Route path="/batches" element={<BatchesPage />} />
            <Route path="/batches/:batchId" element={<BatchDetailPage />} />
            <Route path="/drying" element={<DryingPage />} />
            <Route path="/washing-sorting" element={<WashingSortingPage />} />
            <Route path="/equipment" element={<EquipmentPage />} />
            <Route path="/operators" element={<OperatorsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
