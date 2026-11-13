/**
 * Product Transformation - Compliance Record List Component
 * Displays, filters, creates, edits and deletes HACCP compliance records.
 *
 * Data contract: /api/compliance-records (see ComplianceRecordController).
 * Business rule enforced by the backend: the auditor must have the AUDITOR
 * role, which is why the selector only lists those operators.
 *
 * `initialBatchId` lets another screen (Packaging) open this list already
 * filtered to one batch.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { complianceApi } from '../api/complianceApi';
import { batchApi } from '../api/batchApi';
import { operatorApi } from '../api/operatorApi';
import LoadingState from './LoadingState';
import ErrorState from '../../../shared/components/ErrorState';
import EmptyState from '../../../shared/components/EmptyState';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { getComplianceColor } from '../utils/format';
import { formatEnumLabel } from '../../../shared/utils/formatEnumLabel';
import { todayIsoDate } from '../../../shared/utils/todayIsoDate';
import { Plus, Filter, Eye, Pencil, Trash2, Download } from 'lucide-react';

import { CARD_FRAME, FIELD_INPUT, FIELD_LABEL, FILTER_INPUT, PAGE_TITLE, PRIMARY_BUTTON, SECONDARY_BUTTON, SECTION_CARD, TABLE_CARD, TABLE_HEAD_BASE } from '../styles/ptStyles';

const COMPLIANCE_TYPES = ['HACCP', 'FOOD_SAFETY', 'EU_EXPORT', 'HYGIENE', 'TRACEABILITY'];
const COMPLIANCE_RESULTS = ['COMPLIANT', 'NON_COMPLIANT', 'PENDING'];

const enumLabel = (value) => (value ? formatEnumLabel(value) : '—');

const TYPE_LABELS = {
  HACCP: 'HACCP',
  FOOD_SAFETY: 'Food Safety',
  EU_EXPORT: 'EU Export',
  HYGIENE: 'Hygiene',
  TRACEABILITY: 'Traceability',
};

const RESULT_LABELS = {
  COMPLIANT: 'Compliant',
  NON_COMPLIANT: 'Non-compliant',
  PENDING: 'Pending',
};

const typeLabel = (value) => TYPE_LABELS[value] || enumLabel(value);
const resultLabel = (value) => RESULT_LABELS[value] || enumLabel(value);

/** Date-only values (YYYY-MM-DD) must not shift across time zones. */
const formatIsoDate = (value) => {
  if (!value) return 'N/A';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  if (!year || !month || !day) return String(value);
  return new Date(Number(year), Number(month) - 1, Number(day)).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

const EMPTY_FILTERS = {
  batchId: 'all',
  complianceType: 'all',
  result: 'all',
  auditorId: 'all',
  date: '',
  overdueOnly: false,
};

const emptyForm = () => ({
  recordId: '',
  batchId: '',
  complianceType: 'HACCP',
  result: 'PENDING',
  requirement: '',
  evidence: '',
  auditorId: '',
  auditDate: todayIsoDate(),
  nextAuditDate: '',
});

const escapeCsv = (value) => {
  if (value === null || value === undefined) return '';
  const text = String(value);
  return `"${text.replace(/"/g, '""')}"`;
};

const ComplianceRecordList = ({ initialBatchId = '' }) => {
  const [records, setRecords] = useState([]);
  const [batches, setBatches] = useState([]);
  const [auditors, setAuditors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [filters, setFilters] = useState(() => ({
    ...EMPTY_FILTERS,
    batchId: initialBatchId || 'all',
  }));
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [detail, setDetail] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [actionError, setActionError] = useState('');

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
    // Deferred so the initial setState happens outside the effect body (same
    // pattern as the Water Supply views).
    const timeoutId = window.setTimeout(load, 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const auditorName = useMemo(() => {
    const byId = new Map(auditors.map((operator) => [operator.operatorId, operator.operatorName]));
    return (auditorId) => byId.get(auditorId) || auditorId || '—';
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

  const updateFilter = (key) => (event) => {
    const value = key === 'overdueOnly' ? event.target.checked : event.target.value;
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm());
    setFormError('');
    setActionError('');
    setShowForm(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    setForm({
      recordId: record.recordId || '',
      batchId: record.batchId || '',
      complianceType: record.complianceType || 'HACCP',
      result: record.result || 'PENDING',
      requirement: record.requirement || '',
      evidence: record.evidence || '',
      auditorId: record.auditorId || '',
      auditDate: record.auditDate ? String(record.auditDate).slice(0, 10) : todayIsoDate(),
      nextAuditDate: record.nextAuditDate ? String(record.nextAuditDate).slice(0, 10) : '',
    });
    setFormError('');
    setActionError('');
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;
    setShowForm(false);
    setEditing(null);
    setFormError('');
  };

  const updateForm = (key) => (event) => {
    setForm((current) => ({ ...current, [key]: event.target.value }));
  };

  const validate = () => {
    if (!editing && !form.recordId.trim()) return 'Record ID is required';
    if (!editing && !form.batchId) return 'Batch is required';
    if (!form.complianceType) return 'Compliance type is required';
    if (!form.requirement.trim()) return 'Requirement is required';
    if (!form.result) return 'Result is required';
    if (!form.auditDate) return 'Audit date is required';
    return '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setSaving(true);
    setFormError('');
    try {
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

      setShowForm(false);
      setEditing(null);
      await load();
    } catch (err) {
      setFormError(err?.message || 'Failed to save the compliance record');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    setActionError('');
    try {
      await complianceApi.deleteComplianceRecord(deleteTarget.recordId);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setActionError(err?.message || 'Failed to delete the compliance record');
    } finally {
      setDeleting(false);
    }
  };

  const handleExport = () => {
    const columns = [
      'Record ID',
      'Batch ID',
      'Compliance Type',
      'Requirement',
      'Result',
      'Evidence',
      'Auditor',
      'Audit Date',
      'Next Audit Date',
      'Compliant',
      'Audit Overdue',
    ];
    const lines = [columns.map(escapeCsv).join(',')];
    filteredRecords.forEach((record) => {
      lines.push(
        [
          record.recordId,
          record.batchId,
          record.complianceType,
          record.requirement,
          record.result,
          record.evidence,
          auditorName(record.auditorId),
          record.auditDate,
          record.nextAuditDate,
          record.compliant ? 'Yes' : 'No',
          record.auditOverdue ? 'Yes' : 'No',
        ]
          .map(escapeCsv)
          .join(','),
      );
    });
    const href = `data:text/csv;charset=utf-8,${encodeURIComponent(lines.join('\n'))}`;
    const link = document.createElement('a');
    link.href = href;
    link.download = 'compliance-records.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (loading) return <LoadingState message="Loading compliance records..." />;
  if (error)
    return (
      <ErrorState message={error?.message || 'Failed to load compliance records'} onRetry={load} />
    );

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className={PAGE_TITLE}>Compliance</h1>
          <p className="text-sm text-gray-600 mt-1">
            Track HACCP, food-safety and export compliance records per batch.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={handleExport}
            disabled={filteredRecords.length === 0}
            className={SECONDARY_BUTTON}
          >
            <Download className="w-4 h-4" />
            Export CSV
          </button>
          <button
            type="button"
            onClick={openCreate}
            className={PRIMARY_BUTTON}
          >
            <Plus className="w-4 h-4" />
            New Compliance Record
          </button>
        </div>
      </div>

      {actionError && (
        <div
          role="alert"
          className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
        >
          {actionError}
        </div>
      )}

      {/* Filters */}
      <div className={`${SECTION_CARD} mb-6`}>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex items-center gap-2 text-gray-500">
            <Filter className="w-5 h-5" />
            <span className="text-sm font-medium">Filters</span>
          </div>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Batch
            <select
              aria-label="Filter by batch"
              value={filters.batchId}
              onChange={updateFilter('batchId')}
              className={FILTER_INPUT}
            >
              <option value="all">All batches</option>
              {batches.map((batch) => (
                <option key={batch.batchId} value={batch.batchId}>
                  {batch.batchId}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Type
            <select
              aria-label="Filter by type"
              value={filters.complianceType}
              onChange={updateFilter('complianceType')}
              className={FILTER_INPUT}
            >
              <option value="all">All types</option>
              {COMPLIANCE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {typeLabel(type)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Result
            <select
              aria-label="Filter by result"
              value={filters.result}
              onChange={updateFilter('result')}
              className={FILTER_INPUT}
            >
              <option value="all">All results</option>
              {COMPLIANCE_RESULTS.map((result) => (
                <option key={result} value={result}>
                  {resultLabel(result)}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Auditor
            <select
              aria-label="Filter by auditor"
              value={filters.auditorId}
              onChange={updateFilter('auditorId')}
              className={FILTER_INPUT}
            >
              <option value="all">All auditors</option>
              {auditors.map((operator) => (
                <option key={operator.operatorId} value={operator.operatorId}>
                  {operator.operatorName}
                </option>
              ))}
            </select>
          </label>

          <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
            Audit Date
            <input
              type="date"
              aria-label="Filter by audit date"
              value={filters.date}
              onChange={updateFilter('date')}
              className={FILTER_INPUT}
            />
          </label>

          <label className="flex items-center gap-2 pb-2 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={filters.overdueOnly}
              onChange={updateFilter('overdueOnly')}
            />
            Overdue only
          </label>

          <span className="text-gray-600 text-sm ml-auto">
            {filteredRecords.length} records
          </span>
        </div>
      </div>

      {/* Table */}
      {filteredRecords.length === 0 ? (
        <div className={CARD_FRAME}>
          <EmptyState
            message={records.length === 0 ? 'No compliance records found' : 'No records match the filters'}
            actionLabel={records.length === 0 ? 'New Compliance Record' : undefined}
            onAction={records.length === 0 ? openCreate : undefined}
          />
        </div>
      ) : (
        <div className={TABLE_CARD}>
          <table className="w-full">
            <thead>
              <tr>
                {[
                  'Record',
                  'Batch',
                  'Type',
                  'Result',
                  'Audit Date',
                  'Next Audit',
                  'Auditor',
                  'Status',
                  'Actions',
                ].map((label) => (
                  <th
                    key={label}
                    className={`${TABLE_HEAD_BASE} ${label === 'Actions' ? 'text-right' : 'text-left'}`}
                  >
                    {label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredRecords.map((record) => (
                <tr key={record.recordId} className="hover:bg-gray-50">
                  <td className="px-4 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{record.recordId}</div>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {record.batchId}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900">
                    {typeLabel(record.complianceType)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getComplianceColor(
                        record.result,
                      )}`}
                    >
                      {resultLabel(record.result)}
                    </span>
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {formatIsoDate(record.auditDate)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {record.nextAuditDate ? formatIsoDate(record.nextAuditDate) : '—'}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-600">
                    {auditorName(record.auditorId)}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap">
                    {record.auditOverdue ? (
                      <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-amber-100 text-amber-800">
                        Overdue
                      </span>
                    ) : (
                      <span className="text-xs text-gray-500">Current</span>
                    )}
                  </td>
                  <td className="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      type="button"
                      onClick={() => setDetail(record)}
                      className="text-gray-600 hover:text-gray-800 inline-flex items-center gap-1 mr-3"
                    >
                      <Eye className="w-4 h-4" />
                      View
                    </button>
                    <button
                      type="button"
                      onClick={() => openEdit(record)}
                      className="text-primary hover:text-primary/80 inline-flex items-center gap-1 mr-3"
                    >
                      <Pencil className="w-4 h-4" />
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(record)}
                      className="text-red-600 hover:text-red-700 inline-flex items-center gap-1"
                    >
                      <Trash2 className="w-4 h-4" />
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Detail modal */}
      {detail && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Compliance record details"
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={() => setDetail(null)}
        >
          <div
            className="bg-white rounded-2xl p-6 shadow-card w-full max-w-2xl max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex justify-between items-start mb-4">
              <div>
                <h2 className="font-heading text-xl font-bold text-on-background">{detail.recordId}</h2>
                <p className="text-sm text-gray-600">
                  Batch {detail.batchId} · {typeLabel(detail.complianceType)}
                </p>
              </div>
              <span
                className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getComplianceColor(
                  detail.result,
                )}`}
              >
                {resultLabel(detail.result)}
              </span>
            </div>

            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <DetailItem label="Audit date" value={formatIsoDate(detail.auditDate)} />
              <DetailItem
                label="Next audit"
                value={detail.nextAuditDate ? formatIsoDate(detail.nextAuditDate) : '—'}
              />
              <DetailItem label="Auditor" value={auditorName(detail.auditorId)} />
              <DetailItem
                label="Status"
                value={detail.auditOverdue ? 'Overdue' : detail.compliant ? 'Compliant' : 'Not compliant'}
              />
              <div className="sm:col-span-2">
                <DetailItem label="Requirement" value={detail.requirement} />
              </div>
              <div className="sm:col-span-2">
                <DetailItem label="Evidence" value={detail.evidence || 'No evidence recorded'} />
              </div>
            </dl>

            <div className="flex justify-end pt-6">
              <button
                type="button"
                onClick={() => setDetail(null)}
                className={SECONDARY_BUTTON}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create / Edit modal */}
      {showForm && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={editing ? 'Edit compliance record' : 'New compliance record'}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
          onClick={closeForm}
        >
          <div
            className="bg-white rounded-2xl p-6 shadow-card w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="font-heading text-xl font-bold text-on-background mb-4">
              {editing ? 'Edit Compliance Record' : 'New Compliance Record'}
            </h2>

            {formError && (
              <div
                role="alert"
                className="mb-4 rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700"
              >
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Record ID
                  <input
                    type="text"
                    value={form.recordId}
                    onChange={updateForm('recordId')}
                    disabled={Boolean(editing)}
                    className={`${FIELD_INPUT} disabled:bg-gray-100`}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Batch
                  <select
                    value={form.batchId}
                    onChange={updateForm('batchId')}
                    disabled={Boolean(editing)}
                    className={`${FIELD_INPUT} disabled:bg-gray-100`}
                  >
                    <option value="">Select a batch</option>
                    {batches.map((batch) => (
                      <option key={batch.batchId} value={batch.batchId}>
                        {batch.batchId}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Compliance Type
                  <select
                    value={form.complianceType}
                    onChange={updateForm('complianceType')}
                    className={FIELD_INPUT}
                  >
                    {COMPLIANCE_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {typeLabel(type)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Result
                  <select
                    value={form.result}
                    onChange={updateForm('result')}
                    className={FIELD_INPUT}
                  >
                    {COMPLIANCE_RESULTS.map((result) => (
                      <option key={result} value={result}>
                        {resultLabel(result)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                Requirement
                <textarea
                  value={form.requirement}
                  onChange={updateForm('requirement')}
                  rows={3}
                  className={FIELD_INPUT}
                />
              </label>

              <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                Evidence
                <textarea
                  value={form.evidence}
                  onChange={updateForm('evidence')}
                  rows={2}
                  className={FIELD_INPUT}
                />
              </label>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Audit Date
                  <input
                    type="date"
                    value={form.auditDate}
                    onChange={updateForm('auditDate')}
                    className={FIELD_INPUT}
                  />
                </label>

                <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                  Next Audit Date
                  <input
                    type="date"
                    value={form.nextAuditDate}
                    onChange={updateForm('nextAuditDate')}
                    className={FIELD_INPUT}
                  />
                </label>
              </div>

              <label className={`flex flex-col gap-1 ${FIELD_LABEL}`}>
                Auditor
                <select
                  value={form.auditorId}
                  onChange={updateForm('auditorId')}
                  className={FIELD_INPUT}
                >
                  <option value="">No auditor</option>
                  {auditors.map((operator) => (
                    <option key={operator.operatorId} value={operator.operatorId}>
                      {operator.operatorName}
                    </option>
                  ))}
                </select>
              </label>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={saving}
                  className={SECONDARY_BUTTON}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className={PRIMARY_BUTTON}
                >
                  {saving ? 'Saving...' : editing ? 'Save' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete compliance record"
          message={`Delete record ${deleteTarget.recordId}? This action cannot be undone.`}
          confirmLabel="Delete"
          busy={deleting}
          onConfirm={handleDelete}
          onCancel={() => {
            if (!deleting) setDeleteTarget(null);
          }}
        />
      )}
    </div>
  );
};

function DetailItem({ label, value }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</dt>
      <dd className="mt-1 text-sm text-gray-900 whitespace-pre-wrap">{value || '—'}</dd>
    </div>
  );
}

export default ComplianceRecordList;
