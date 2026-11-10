/**
 * Product Transformation - Packaging screen adapter
 *
 * Gives the list a navigation callback: choosing "Compliance" on a row opens
 * the Compliance tab filtered to that row's batch.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useNavigate } from 'react-router-dom';
import PackagingRecordList from './PackagingRecordList';

export default function PackagingScreen() {
  const navigate = useNavigate();

  const openCompliance = (batchId) => {
    if (!batchId) {
      navigate('/product-transformation/compliance');
      return;
    }
    navigate(`/product-transformation/compliance?batchId=${encodeURIComponent(batchId)}`);
  };

  return <PackagingRecordList onOpenCompliance={openCompliance} />;
}
