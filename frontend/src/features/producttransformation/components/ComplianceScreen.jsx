/**
 * Product Transformation - Compliance screen adapter
 *
 * Reads the optional `batchId` query parameter so that the Packaging screen can
 * open this list already filtered to a batch. The key forces a remount when the
 * requested batch changes, keeping the filter in sync with the URL.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useSearchParams } from 'react-router-dom';
import ComplianceRecordList from './ComplianceRecordList';

export default function ComplianceScreen() {
  const [searchParams] = useSearchParams();
  const batchId = searchParams.get('batchId') || '';

  return <ComplianceRecordList key={batchId || 'all'} initialBatchId={batchId} />;
}
