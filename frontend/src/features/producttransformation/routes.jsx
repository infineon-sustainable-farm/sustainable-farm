import ProductTransformationLayout from './components/ProductTransformationLayout';
import Dashboard from './components/Dashboard';
import BatchList from './components/BatchList';
import QcCheckpointList from './components/QcCheckpointList';
import ComplianceScreen from './components/ComplianceScreen';
import PackagingScreen from './components/PackagingScreen';

/**
 * Product Transformation routes.
 *
 * The module follows the same shell as machinery, plants and watersupply:
 * `/product-transformation` is the module home, and each tab is a real URL
 * rendered inside ProductTransformationLayout. There is one sidebar per
 * module, owned by the layout — no second navigation inside the screens.
 */
export default [
  {
    path: 'product-transformation',
    element: <ProductTransformationLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'batches', element: <BatchList /> },
      { path: 'quality-control', element: <QcCheckpointList /> },
      { path: 'compliance', element: <ComplianceScreen /> },
      { path: 'packaging', element: <PackagingScreen /> },
      {
        path: 'settings',
        element: (
          <div className="p-6">
            <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
            <p className="text-gray-600 mt-2">Settings module coming soon...</p>
          </div>
        ),
      },
    ],
  },
];
