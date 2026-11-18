/**
 * Product Transformation - Compliance screen
 * Displays, filters, creates, edits and deletes HACCP compliance records.
 *
 * Data contract: /api/product-transformation/compliance-records (see ComplianceRecordController).
 * Business rule enforced by the backend: the auditor must have the AUDITOR
 * role, which is why the selector only lists those operators.
 *
 * `initialBatchId` lets another screen (Packaging) open this list already
 * filtered to one batch.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 2.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Download, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, ConfirmDialog, EmptyState, ErrorState, PageHeader } from '../../../shared/ui';
import { complianceApi } from '../api/complianceApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';
import ComplianceDetailModal from './ComplianceDetailModal';
import ComplianceFilters from './ComplianceFilters';
import ComplianceFormModal from './ComplianceFormModal';
import ComplianceTable from './ComplianceTable';
import { buildComplianceCsv, complianceNone, downloadCsv } from './complianceOptions';

const EMPTY_FILTERS = {
  batchId: 'all',
  complianceType: 'all',
  result: 'all',
  auditorId: 'all',
  date: '',
  overdueOnly: false,
};

const ComplianceRecordList = ({ initialBatchId = '' }) => {
  const { t } = useTranslation('producttransformation');
  const [records, setRecords] = useState([]);
  const [batches, setBatches] = useState([]);
  const [auditors, setAuditors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(() => ({ ...EMPTY_FILTERS, batchId: initialBatchId || 'all' }));
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const [detail, setDetail] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [recordData, batchData, auditorData] = await Promise.all([
        complianceApi.getAllComplianceRecords(),
        batchApi.getAllBatches(),
        operatorApi.getOperatorsByRole('AUDITOR'),
      ]);
      setRecords(Array.isArray(recordData) ? recordData : []);
      setBatches(Array.isArray(batchData) ? batchData : []);
      setAuditors(Array.isArray(auditorData) ? auditorData : []);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Deferred so the initial setState happens outside the effect body.
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const auditorName = useMemo(() => {
    const byId = new Map(auditors.map((operator) => [operator.operatorId, operator.operatorName]));
    return (auditorId) => byId.get(auditorId) || auditorId || complianceNone();
  }, [auditors]);

  const filteredRecords = useMemo(() => {
    return records
      .filter((record) => {
        if (filters.batchId !== 'all' && record.batchId !== filters.batchId) return false;
        if (filters.complianceType !== 'all' && record.complianceType !== filters.complianceType)
          return false;
        if (filters.result !== 'all' && record.result !== filters.result) return false;
        if (filters.auditorId !== 'all' && record.auditorId !== filters.auditorId) return false;
        if (filters.date && String(record.auditDate || '').slice(0, 10) !== filters.date) return false;
        if (filters.overdueOnly && !record.auditOverdue) return false;
        return true;
      })
      .sort((a, b) => String(b.auditDate || '').localeCompare(String(a.auditDate || '')));
  }, [records, filters]);

  const updateFilter = (key, value) => setFilters((current) => ({ ...current, [key]: value }));

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  const handleSubmit = async (form) => {
    const payload = {
      complianceType: form.complianceType,
      requirement: form.requirement.trim(),
      result: form.result,
      evidence: form.evidence.trim() || null,
      auditorId: form.auditorId || null,
      auditDate: form.auditDate,
      nextAuditDate: form.nextAuditDate || null,
    };

    if (editing) {
      await complianceApi.updateComplianceRecord(editing.recordId, payload);
    } else {
      await complianceApi.createComplianceRecord({
        recordId: form.recordId.trim(),
        batchId: form.batchId,
        ...payload,
      });
    }

    closeForm();
    await load();
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await complianceApi.deleteComplianceRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setDeleteError(err?.message || t('compliance.deleteError'));
    } finally {
      setDeleting(false);
    }
  };

  const closeDelete = () => {
    if (deleting) return;
    setDeleteTarget(null);
    setDeleteError('');
  };

  const handleExport = () => downloadCsv(buildComplianceCsv(filteredRecords, auditorName));

  const renderContent = () => {
    if (loading) return <ComplianceTable loading />;
    if (error) {
      return (
        <ErrorState
          message={error?.message || t('compliance.loadError')}
          onRetry={load}
          retryLabel={t('common.tryAgain')}
        />
      );
    }
    return (
      <>
        <ComplianceFilters
          filters={filters}
          onChange={updateFilter}
          batches={batches}
          auditors={auditors}
          count={filteredRecords.length}
        />
        {filteredRecords.length === 0 ? (
          <EmptyState
            message={records.length === 0 ? t('compliance.empty') : t('compliance.noMatch')}
            actionLabel={records.length === 0 ? t('compliance.new') : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        ) : (
          <ComplianceTable
            records={filteredRecords}
            auditorName={auditorName}
            onView={setDetail}
            onEdit={openEdit}
            onDelete={setDeleteTarget}
          />
        )}
      </>
    );
  };

  return (
    <div className="p-6">
      <PageHeader
        className="mb-6"
        title={t('compliance.title')}
        subtitle={t('compliance.subtitle')}
        actions={
          <>
            <Button
              variant="secondary"
              icon={Download}
              onClick={handleExport}
              disabled={loading || Boolean(error) || filteredRecords.length === 0}
            >
              {t('compliance.exportCsv')}
            </Button>
            <Button icon={Plus} onClick={openCreate}>
              {t('compliance.new')}
            </Button>
          </>
        }
      />

      {renderContent()}

      <ComplianceDetailModal record={detail} auditorName={auditorName} onClose={() => setDetail(null)} />

      {showForm && (
        <ComplianceFormModal
          key={editing?.recordId ?? 'create'}
          open
          editing={editing}
          batches={batches}
          auditors={auditors}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('compliance.deleteTitle')}
        message={deleteTarget ? t('compliance.deleteMessage', { id: deleteTarget.recordId }) : ''}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        loading={deleting}
        error={deleteError || null}
        onConfirm={handleDelete}
        onClose={closeDelete}
      />
    </div>
  );
};

export default ComplianceRecordList;
