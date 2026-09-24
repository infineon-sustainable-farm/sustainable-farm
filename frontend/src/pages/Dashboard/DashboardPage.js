import React from 'react';

import { dashboardApi } from '../../services/api';
import Dashboard from '../../components/Dashboard';
import { KPICardSkeleton } from '../../components/common/LoadingSkeleton';
import ErrorMessage from '../../components/common/ErrorMessage';
import PlantsInfo from '../../components/PlantsInfo';

import './DashboardPage.css';

const DashboardPage = () => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  React.useEffect(() => {
    fetchDashboardKPIs();
  }, []);

  const fetchDashboardKPIs = async () => {
    try {
      setLoading(true);

      await dashboardApi.getDashboardKPIs();

      setError(null);
    } catch (err) {
      console.error('Error fetching dashboard KPIs:', err);
      setError('Failed to load dashboard data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <KPICardSkeleton count={11} />;
  }

  if (error) {
    return (
      <ErrorMessage
        message={error}
        onRetry={fetchDashboardKPIs}
      />
    );
  }

  return (
    <div className="dashboard-page">
      <Dashboard />
      
      {/* Plants Integration - Show all varieties */}
      <PlantsInfo showGrowthCalendar={false} />
    </div>
  );
};

export default DashboardPage;
