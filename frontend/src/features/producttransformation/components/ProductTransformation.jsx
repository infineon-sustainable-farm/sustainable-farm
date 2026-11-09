/**
 * Product Transformation - Main Component
 * Entry point for Product Transformation module with navigation
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useState } from 'react';
import Dashboard from './Dashboard';
import BatchList from './BatchList';
import { LayoutDashboard, Package, CheckCircle, FileText, Settings } from 'lucide-react';

const ProductTransformation = () => {
  const [activeTab, setActiveTab] = useState('dashboard');

  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'batches', label: 'Batches', icon: Package },
    { id: 'qc', label: 'Quality Control', icon: CheckCircle },
    { id: 'compliance', label: 'Compliance', icon: FileText },
    { id: 'settings', label: 'Settings', icon: Settings },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'batches':
        return <BatchList />;
      case 'qc':
        return <div className="p-6"><h1 className="text-2xl font-bold text-gray-900">Quality Control</h1><p className="text-gray-600 mt-2">QC module coming soon...</p></div>;
      case 'compliance':
        return <div className="p-6"><h1 className="text-2xl font-bold text-gray-900">Compliance</h1><p className="text-gray-600 mt-2">Compliance module coming soon...</p></div>;
      case 'settings':
        return <div className="p-6"><h1 className="text-2xl font-bold text-gray-900">Settings</h1><p className="text-gray-600 mt-2">Settings module coming soon...</p></div>;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <div className="w-64 bg-white shadow-sm">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-bold text-gray-900">Product Transformation</h2>
          <p className="text-sm text-gray-600">Mango Processing</p>
        </div>
        <nav className="p-4">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg mb-2 transition-colors ${
                  activeTab === tab.id
                    ? 'bg-primary text-white'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <Icon className="w-5 h-5" />
                <span className="font-medium">{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        {renderContent()}
      </div>
    </div>
  );
};

export default ProductTransformation;
