/**
 * Product Transformation - Dashboard Component
 * Displays key metrics and statistics for Product Transformation
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useApi } from '../../../shared/hooks/useApi';
import { dashboardApi } from '../api/dashboardApi';
import LoadingState from '../../../shared/components/LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import { formatWeight, formatPercentage } from '../../../shared/utils/formatUtils';
import { Package, TrendingUp, CheckCircle, AlertCircle } from 'lucide-react';

const Dashboard = () => {
  const { data: stats, loading, error, refetch } = useApi('/api/product-transformation/dashboard/stats');

  if (loading) return <LoadingState message="Loading dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  const metrics = stats || {
    totalBatches: 0,
    activeBatches: 0,
    completedBatches: 0,
    totalHarvestQuantityKg: 0,
    qcPassRate: 0,
    avgProcessingTimeDays: 0,
  };

  const statCards = [
    {
      title: 'Total Batches',
      value: metrics.totalBatches,
      icon: Package,
      color: 'bg-blue-500',
      format: 'number',
    },
    {
      title: 'Active Batches',
      value: metrics.activeBatches,
      icon: TrendingUp,
      color: 'bg-green-500',
      format: 'number',
    },
    {
      title: 'Completed Batches',
      value: metrics.completedBatches,
      icon: CheckCircle,
      color: 'bg-purple-500',
      format: 'number',
    },
    {
      title: 'Total Harvest',
      value: metrics.totalHarvestQuantityKg,
      icon: Package,
      color: 'bg-orange-500',
      format: 'weight',
    },
    {
      title: 'QC Pass Rate',
      value: metrics.qcPassRate,
      icon: CheckCircle,
      color: 'bg-emerald-500',
      format: 'percentage',
    },
    {
      title: 'Avg Processing Time',
      value: metrics.avgProcessingTimeDays,
      icon: AlertCircle,
      color: 'bg-red-500',
      format: 'days',
    },
  ];

  const formatValue = (value, format) => {
    switch (format) {
      case 'weight':
        return formatWeight(value);
      case 'percentage':
        return formatPercentage(value);
      case 'days':
        return `${value} days`;
      default:
        return value;
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Product Transformation Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.title} className="bg-white rounded-lg shadow-sm p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                  <p className="text-2xl font-bold text-gray-900 mt-2">
                    {formatValue(stat.value, stat.format)}
                  </p>
                </div>
                <div className={`${stat.color} p-3 rounded-lg`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <Package className="w-6 h-6 text-primary mb-2" />
            <p className="font-medium text-gray-900">Create New Batch</p>
            <p className="text-sm text-gray-600">Start a new processing batch</p>
          </button>
          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <CheckCircle className="w-6 h-6 text-primary mb-2" />
            <p className="font-medium text-gray-900">View QC Status</p>
            <p className="text-sm text-gray-600">Check quality control checkpoints</p>
          </button>
          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <TrendingUp className="w-6 h-6 text-primary mb-2" />
            <p className="font-medium text-gray-900">View Reports</p>
            <p className="text-sm text-gray-600">Generate processing reports</p>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
