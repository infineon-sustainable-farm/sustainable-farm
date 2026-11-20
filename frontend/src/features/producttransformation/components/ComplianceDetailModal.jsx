import { useTranslation } from 'react-i18next';
import { Button, Modal, StatusBadge } from '../../../shared/ui';
import { formatIsoDate, getComplianceTone } from '../utils/format';
import {
  complianceNone,
  complianceResultLabel,
  complianceTypeLabel,
  nextAuditLabel,
} from './complianceOptions';

function DetailItem({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium text-text-muted">{label}</dt>
      <dd className="mt-1 whitespace-pre-wrap text-sm">{value || complianceNone()}</dd>
    </div>
  );
}

/** Every field of one compliance record, read-only. */
export default function ComplianceDetailModal({ record, auditorName, onClose }) {
  const { t } = useTranslation('producttransformation');
  if (!record) return null;

  const status = record.auditOverdue
    ? t('compliance.overdue')
    : record.compliant
      ? t('compliance.compliant')
      : t('compliance.notCompliant');

  return (
    <Modal
      open
      onClose={onClose}
      size="xl"
      title={record.recordId}
      description={t('compliance.detail.subtitle', {
        batch: record.batchId,
        type: complianceTypeLabel(record.complianceType),
      })}
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('compliance.detail.close')}
        </Button>
      }
    >
      <div className="mb-4">
        <StatusBadge tone={getComplianceTone(record.result)} label={complianceResultLabel(record.result)} />
      </div>
      <dl className="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
        <DetailItem label={t('compliance.detail.auditDate')} value={formatIsoDate(record.auditDate)} />
        <DetailItem label={t('compliance.detail.nextAudit')} value={nextAuditLabel(record.nextAuditDate)} />
        <DetailItem label={t('compliance.detail.auditor')} value={auditorName(record.auditorId)} />
        <DetailItem label={t('compliance.detail.status')} value={status} />
        <div className="sm:col-span-2">
          <DetailItem label={t('compliance.detail.requirement')} value={record.requirement} />
        </div>
        <div className="sm:col-span-2">
          <DetailItem
            label={t('compliance.detail.evidence')}
            value={record.evidence || t('compliance.detail.noEvidence')}
          />
        </div>
      </dl>
    </Modal>
  );
}
