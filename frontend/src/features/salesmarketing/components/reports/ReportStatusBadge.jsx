import { CircleAlert, CircleCheck, Clock, RefreshCw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Badge, StatusBadge } from '../../../../shared/ui';

/** State of a report: ready, processing, scheduled, failed, or the raw status. */
function ReportStatusBadge({ status }) {
  const { t } = useTranslation('salesmarketing');
  const s = (status || '').toLowerCase();
  if (s.includes('ready') || s.includes('complete') || s.includes('done')) {
    return <StatusBadge tone="success" icon={CircleCheck} withIcon label={t('reports.badge.ready')} />;
  }
  if (s.includes('process') || s.includes('running') || s.includes('pending')) {
    return <StatusBadge tone="neutral" icon={RefreshCw} withIcon label={t('reports.badge.processing')} />;
  }
  if (s.includes('schedul')) {
    return <StatusBadge tone="warning" icon={Clock} withIcon label={t('reports.badge.scheduled')} />;
  }
  if (s.includes('fail') || s.includes('error')) {
    return <StatusBadge tone="error" icon={CircleAlert} withIcon label={t('reports.badge.failed')} />;
  }
  return <Badge size="md">{status || t('reports.badge.unknown')}</Badge>;
}

export default ReportStatusBadge;
