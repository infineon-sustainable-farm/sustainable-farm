import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from './features/salesmarketing/components/layout/MainLayout';
import DashboardPage from './features/salesmarketing/pages/DashboardPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
