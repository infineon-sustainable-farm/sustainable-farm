import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { Button, ConfirmDialog, PageHeader, Skeleton } from '../../../shared/ui';
import Notice from '../components/common/Notice';
import ReportActivityTable from '../components/reports/ReportActivityTable';
import { NewScheduleCard, ReportCard } from '../components/reports/ReportCard';
import ReportFilters from '../components/reports/ReportFilters';
import ReportFormModal from '../components/reports/ReportFormModal';
import {
  downloadReportCsv,
  emptyReportForm,
  getErrorMessage,
  reportStatus,
  resolveCategory,
  useReportText,
} from '../components/reports/reportUtils';

export default function DemandReportsPage() {
  const { t } = useTranslation('salesmarketing');
  const text = useReportText();
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [reports, setReports] = useState([]);
  const [reportFiles, setReportFiles] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState('all');
  const [selectedStatus, setSelectedStatus] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const [showReportModal, setShowReportModal] = useState(false);
  const [editingReportId, setEditingReportId] = useState(null);
  const [reportForm, setReportForm] = useState(emptyReportForm());
  const [savingReport, setSavingReport] = useState(false);

  const [pendingDeleteReport, setPendingDeleteReport] = useState(null);
  const [deletingReport, setDeletingReport] = useState(false);

  const flashSuccess = (msg) => {
    setActionSuccess(msg);
    setActionError(null);
    window.setTimeout(() => setActionSuccess(null), 4000);
  };

  const flashError = (msg) => {
    setActionError(msg);
    setActionSuccess(null);
    window.setTimeout(() => setActionError(null), 6000);
  };

  const fetchReportsData = useCallback(async () => {
    const requestFailed = t('reports.requestFailed');
    try {
      const [reportsRes, filesRes] = await Promise.allSettled([api.getDemandReports(), api.getReportFiles()]);
      setApiError(null);

      const failures = [];

      if (reportsRes.status === 'fulfilled') {
        setReports(Array.isArray(reportsRes.value) ? reportsRes.value : []);
      } else {
        setReports([]);
        failures.push(t('reports.failureReports', { message: getErrorMessage(reportsRes.reason, requestFailed) }));
      }

      if (filesRes.status === 'fulfilled') {
        setReportFiles(Array.isArray(filesRes.value) ? filesRes.value : []);
      } else {
        setReportFiles([]);
        failures.push(t('reports.failureFiles', { message: getErrorMessage(filesRes.reason, requestFailed) }));
      }

      if (failures.length > 0) {
        setApiError(
          reportsRes.status === 'rejected'
            ? getErrorMessage(reportsRes.reason, t('reports.loadFailed'))
            : t('reports.partialLoad', { failures: failures.join(' · ') })
        );
      }
    } catch (err) {
      setApiError(getErrorMessage(err, t('reports.loadFailed')));
      setReports([]);
      setReportFiles([]);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void Promise.resolve().then(fetchReportsData);
  }, [fetchReportsData]);

  const filteredReports = reports.filter((report) => {
    if (selectedFilter !== 'all' && resolveCategory(report.reportType) !== selectedFilter) return false;
    if (selectedStatus !== 'all' && reportStatus(report).toLowerCase() !== selectedStatus.toLowerCase()) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const fields = [text.title(report), text.summary(report), report.reportType || '', report.generatedBy || ''];
      if (!fields.some((field) => field.toLowerCase().includes(q))) return false;
    }
    return true;
  });

  const getFilesForReport = (reportId) => reportFiles.filter((f) => f.reportId === reportId);

  const openCreateReport = (asScheduled = false) => {
    const form = emptyReportForm();
    if (asScheduled) {
      form.isScheduled = true;
      form.status = 'scheduled';
      const nextWeek = new Date();
      nextWeek.setDate(nextWeek.getDate() + 7);
      nextWeek.setHours(8, 0, 0, 0);
      form.nextRunAt = nextWeek.toISOString().slice(0, 16);
      form.scheduleFrequency = 'Weekly';
    }
    setReportForm(form);
    setEditingReportId(null);
    setShowReportModal(true);
    setActionError(null);
  };

  const openEditReport = async (report) => {
    setActionError(null);
    setSavingReport(true);
    try {
      const res = await api.getDemandReportById(report.reportId);
      const r = res.data || report;
      setReportForm({
        reportType: r.reportType || 'Forecasting',
        periodStart: r.periodStart ? new Date(r.periodStart).toISOString().slice(0, 10) : '',
        periodEnd: r.periodEnd ? new Date(r.periodEnd).toISOString().slice(0, 10) : '',
        totalForecastedVolumeT: r.totalForecastedVolumeT != null ? String(r.totalForecastedVolumeT) : '',
        totalActualVolumeT: r.totalActualVolumeT != null ? String(r.totalActualVolumeT) : '',
        variancePct: r.variancePct != null ? String(r.variancePct) : '',
        status: r.status || 'ready',
        isScheduled: !!r.isScheduled,
        nextRunAt: r.nextRunAt ? new Date(r.nextRunAt).toISOString().slice(0, 16) : '',
        scheduleFrequency: r.scheduleFrequency || '',
        summary: r.summary || '',
        generatedBy: r.generatedBy || '',
      });
      setEditingReportId(report.reportId);
      setShowReportModal(true);
    } catch (err) {
      flashError(getErrorMessage(err, t('reports.loadForEditFailed', { id: report.reportId })));
    } finally {
      setSavingReport(false);
    }
  };

  const handleSaveReport = async (e) => {
    e.preventDefault();
    setSavingReport(true);
    setActionError(null);
    try {
      const payload = {
        reportType: reportForm.reportType || null,
        periodStart: reportForm.periodStart,
        periodEnd: reportForm.periodEnd,
        totalForecastedVolumeT: reportForm.totalForecastedVolumeT !== '' ? Number(reportForm.totalForecastedVolumeT) : null,
        totalActualVolumeT: reportForm.totalActualVolumeT !== '' ? Number(reportForm.totalActualVolumeT) : null,
        variancePct: reportForm.variancePct !== '' ? Number(reportForm.variancePct) : null,
        status: reportForm.status || null,
        isScheduled: reportForm.isScheduled,
        nextRunAt: reportForm.nextRunAt || null,
        scheduleFrequency: reportForm.scheduleFrequency || null,
        summary: reportForm.summary || null,
        generatedBy: reportForm.generatedBy || null,
        generatedAt: editingReportId != null ? undefined : new Date().toISOString(),
      };

      if (editingReportId != null) {
        await api.updateDemandReport(editingReportId, payload);
        flashSuccess(t('reports.updated', { id: editingReportId }));
      } else {
        const created = await api.createDemandReport(payload);
        flashSuccess(t('reports.created', { id: created.data?.reportId || t('reports.newId') }));
      }

      setShowReportModal(false);
      await fetchReportsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingReportId != null ? t('reports.updateFailed') : t('reports.createFailed')));
    } finally {
      setSavingReport(false);
    }
  };

  const confirmDeleteReport = async () => {
    if (!pendingDeleteReport) return;
    setDeletingReport(true);
    try {
      await api.deleteDemandReport(pendingDeleteReport.reportId);
      const id = pendingDeleteReport.reportId;
      setPendingDeleteReport(null);
      flashSuccess(t('reports.deleted', { id }));
      await fetchReportsData();
    } catch (err) {
      flashError(getErrorMessage(err, t('reports.deleteFailed')));
    } finally {
      setDeletingReport(false);
    }
  };

  const handleExportCsv = (report) => {
    const csvFile = getFilesForReport(report.reportId).find((f) => (f.format || '').toLowerCase() === 'csv');
    if (csvFile && csvFile.fileUrl) {
      window.open(csvFile.fileUrl, '_blank');
      flashSuccess(t('reports.csvInitiated'));
      return;
    }
    downloadReportCsv(report);
    flashSuccess(t('reports.csvDownloaded'));
  };

  const handleExportPdf = (report) => {
    const pdfFile = getFilesForReport(report.reportId).find((f) => (f.format || '').toLowerCase() === 'pdf');
    if (pdfFile && pdfFile.fileUrl) {
      window.open(pdfFile.fileUrl, '_blank');
      flashSuccess(t('reports.pdfInitiated'));
      return;
    }
    flashError(t('reports.noPdf'));
  };

  const showAll = () => {
    setSelectedFilter('all');
    setSelectedStatus('all');
    setSearchQuery('');
    flashSuccess(t('reports.showingAll'));
  };

  return (
    <div className="space-y-6 p-6">
      {apiError && (
        <Notice tone="offline" title={t('reports.apiNotice')} onRetry={fetchReportsData}>
          {apiError}
        </Notice>
      )}
      {actionError && (
        <Notice tone="error" title={t('reports.actionFailed')} onDismiss={() => setActionError(null)}>
          {actionError}
        </Notice>
      )}
      {actionSuccess && (
        <Notice tone="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Notice>
      )}

      <PageHeader
        title={t('reports.title')}
        subtitle={t('reports.subtitle')}
        actions={
          <>
            <Button variant="secondary" icon={CalendarDays} onClick={() => openCreateReport(true)}>
              {t('reports.scheduleReport')}
            </Button>
            <Button icon={Plus} onClick={() => openCreateReport(false)}>
              {t('reports.newCustom')}
            </Button>
          </>
        }
      />

      <ReportFilters
        category={selectedFilter}
        onCategoryChange={setSelectedFilter}
        search={searchQuery}
        onSearchChange={setSearchQuery}
        status={selectedStatus}
        onStatusChange={setSelectedStatus}
      />

      {loading ? (
        <div aria-busy="true" className="space-y-4">
          <p role="status" className="text-sm text-text-muted">
            {t('reports.loading')}
          </p>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[0, 1, 2, 3].map((index) => (
              <Skeleton key={index} radius="card" className="h-72" />
            ))}
          </div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filteredReports.map((report, idx) => (
              <ReportCard
                key={report.reportId || idx}
                report={report}
                index={idx}
                onEdit={openEditReport}
                onDelete={setPendingDeleteReport}
                onExportPdf={handleExportPdf}
                onExportCsv={handleExportCsv}
              />
            ))}
            <NewScheduleCard onClick={() => openCreateReport(true)} />
          </div>

          <ReportActivityTable
            reports={filteredReports}
            onShowAll={showAll}
            onExportPdf={handleExportPdf}
            onExportCsv={handleExportCsv}
            onEdit={openEditReport}
            onDelete={setPendingDeleteReport}
          />
        </>
      )}

      <ReportFormModal
        open={showReportModal}
        editingId={editingReportId}
        form={reportForm}
        setForm={setReportForm}
        saving={savingReport}
        onClose={() => setShowReportModal(false)}
        onSubmit={handleSaveReport}
      />

      <ConfirmDialog
        open={Boolean(pendingDeleteReport)}
        onClose={() => setPendingDeleteReport(null)}
        onConfirm={confirmDeleteReport}
        loading={deletingReport}
        title={t('reports.deleteTitle')}
        confirmLabel={deletingReport ? t('reports.deleting') : t('common:actions.delete')}
        message={
          pendingDeleteReport && (
            <>
              <p>{t('reports.deleteMessage', { title: text.title(pendingDeleteReport) })}</p>
              <p className="mt-2 line-clamp-3">{text.summary(pendingDeleteReport)}</p>
            </>
          )
        }
      />
    </div>
  );
}
