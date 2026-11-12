import ProductTransformationLayout from './components/ProductTransformationLayout';
import Dashboard from './components/Dashboard';
import RawIntakeList from './components/RawIntakeList';
import BatchList from './components/BatchList';
import BatchDetail from './components/BatchDetail';
import WashSortRecordList from './components/WashSortRecordList';
import DryingRunList from './components/DryingRunList';
import QcCheckpointList from './components/QcCheckpointList';
import ComplianceScreen from './components/ComplianceScreen';
import PackagingScreen from './components/PackagingScreen';
import EquipmentList from './components/EquipmentList';
import OperatorList from './components/OperatorList';

/**
 * Product Transformation routes.
 *
 * The module follows the same shell as machinery, plants and watersupply:
 * `/product-transformation` is the module home, and each tab is a real URL
 * rendered inside ProductTransformationLayout. There is one sidebar per
 * module, owned by the layout — no second navigation inside the screens.
 *
 * Screen order follows the processing line: reception, batch, washing,
 * drying, quality control, compliance, packaging, then the equipment registry
 * and the operators those screens reference.
 */
export default [
  {
    path: 'product-transformation',
    element: <ProductTransformationLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'raw-intake', element: <RawIntakeList /> },
      { path: 'batches', element: <BatchList /> },
      { path: 'batches/:batchId', element: <BatchDetail /> },
      { path: 'washing-sorting', element: <WashSortRecordList /> },
      { path: 'drying', element: <DryingRunList /> },
      { path: 'quality-control', element: <QcCheckpointList /> },
      { path: 'compliance', element: <ComplianceScreen /> },
      { path: 'packaging', element: <PackagingScreen /> },
      { path: 'equipment', element: <EquipmentList /> },
      { path: 'operators', element: <OperatorList /> },
    ],
  },
];
