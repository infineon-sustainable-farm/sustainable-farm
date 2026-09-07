import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DashboardPage from './pages/Dashboard/DashboardPage';
import HarvestPage from './pages/Harvest/HarvestPage';
import RawIntakePage from './pages/RawIntake/RawIntakePage';
import BatchesPage from './pages/Batches/BatchesPage';
import BatchDetailPage from './pages/Batches/BatchDetailPage';
import DryingPage from './pages/Drying/DryingPage';
import WashingSortingPage from './pages/WashingSorting/WashingSortingPage';
import EquipmentPage from './pages/Equipment/EquipmentPage';
import OperatorsPage from './pages/Operators/OperatorsPage';
import './App.css';

function App() {
  return (
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
  );
}

export default App;