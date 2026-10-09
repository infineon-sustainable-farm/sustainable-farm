import React, { useEffect, useState } from 'react';
import { api } from '../api/api';

const FILTERS = [
  { id: 'all', label: 'All Reports', icon: 'list_alt' },
  { id: 'forecasting', label: 'Forecasting', icon: 'trending_up' },
  { id: 'regional', label: 'Regional', icon: 'map' },
  { id: 'risk', label: 'Risk Analysis', icon: 'warning' },
];

const REPORT_TYPES = ['Forecasting', 'Regional', 'Risk Analysis', 'Sales Performance', 'Inventory'];
const STATUS_OPTIONS = ['ready', 'processing', 'scheduled', 'failed'];
const SCHEDULE_FREQUENCIES = ['Daily', 'Weekly', 'Monthly', 'Quarterly'];

function getErrorMessage(err, fallback = 'Request failed') {
  const data = err?.response?.data;
  if (!data) return err?.message || fallback;
  if (typeof data === 'string') return data;
  if (data.message) {
    if (data.validationErrors && typeof data.validationErrors === 'object') {
      const details = Object.values(data.validationErrors).join(', ');
      return details ? `${data.message}: ${details}` : data.message;
    }
    return data.message;
  }
  return fallback;
}

function emptyReportForm() {
  const today = new Date();
  const nextMonth = new Date(today);
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  return {
    reportType: 'Forecasting',
    periodStart: today.toISOString().slice(0, 10),
    periodEnd: nextMonth.toISOString().slice(0, 10),
    totalForecastedVolumeT: '',
    totalActualVolumeT: '',
    variancePct: '',
    status: 'ready',
    isScheduled: false,
    nextRunAt: '',
    scheduleFrequency: '',
    summary: '',
    generatedBy: '',
  };
}

function formatPeriodLabel(report) {
  if (report.isScheduled && report.nextRunAt) {
    const d = new Date(report.nextRunAt);
    if (!isNaN(d.getTime())) {
      return `Next: ${d.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })}`;
    }
  }
  if (report.generatedAt) {
    const d = new Date(report.generatedAt);
    if (!isNaN(d.getTime())) {
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
  }
  if (report.periodStart && report.periodEnd) {
    return `${report.periodStart} ââ€ â€™ ${report.periodEnd}`;
  }
  return 'Current Period';
}

function resolveTitle(report) {
  if (report.title) return report.title;
  if (report.reportType) return `${report.reportType} Report`;
  return `Report #${report.reportId}`;
}

function resolveSummary(report) {
  if (report.summary) return report.summary;
  const vol = report.totalForecastedVolumeT != null
    ? `${Number(report.totalForecastedVolumeT).toLocaleString()} t forecasted`
    : null;
  const variance = report.variancePct != null
    ? `${Number(report.variancePct)}% variance`
    : null;
  return [vol, variance].filter(Boolean).join(' · ') || 'Demand report for Dried Mango exports.';
}

function resolveCategory(reportType = '') {
  const t = reportType.toLowerCase();
  if (t.includes('region') || t.includes('germany') || t.includes('regional')) return 'regional';
  if (t.includes('risk') || t.includes('alert')) return 'risk';
  if (t.includes('forecast') || t.includes('scenario') || t.includes('demand')) return 'forecasting';
  return 'forecasting';
}

function getCardVisuals(report, index) {
  const category = resolveCategory(report.reportType);
  const presets = {
    forecasting: [
      { icon: 'troubleshoot', iconTone: 'primary', accent: 'secondary', isAutoGen: true, csvEnabled: true },
      { icon: 'public', iconTone: 'tertiary', accent: 'primary', isAutoGen: false, csvEnabled: true },
    ],
    regional: [
      { icon: 'warning', iconTone: 'error', accent: 'error', isAutoGen: false, csvEnabled: false },
      { icon: 'map', iconTone: 'primary', accent: 'primary', isAutoGen: false, csvEnabled: true },
    ],
    risk: [
      { icon: 'warning', iconTone: 'error', accent: 'error', isAutoGen: false, csvEnabled: true },
    ],
  };
  const list = presets[category] || presets.forecasting;
  const preset = list[index % list.length];
  const generatedBy = (report.generatedBy || '').toLowerCase();
  const isAutoGen = !!report.isScheduled || generatedBy === 'system' || generatedBy === 'auto';
  return {
    ...preset,
    isAutoGen,
    csvEnabled: true,
  };
}

function iconToneClasses(tone) {
  switch (tone) {
    case 'tertiary':
      return 'bg-tertiary-container/20 text-tertiary';
    case 'error':
      return 'bg-error-container/30 text-error';
    case 'surface':
      return 'bg-surface-container text-on-surface-variant';
    default:
      return 'bg-primary-container/20 text-primary';
  }
}

function accentBlobClass(accent) {
  switch (accent) {
    case 'secondary':
      return 'bg-secondary/5';
    case 'error':
      return 'bg-error/5';
    default:
      return 'bg-primary/5';
  }
}

function StatusBadge({ status }) {
  const s = (status || '').toLowerCase();
  if (s.includes('ready') || s.includes('complete') || s.includes('done')) {
    return (
      <span className="inline-flex items-center gap-xs px-sm py-unit rounded-full bg-[#e8f5e9] text-[#1b5e20] font-label-md text-label-md">
        <span className="material-symbols-outlined text-[16px]">check_circle</span>
        Ready
      </span>
    );
  }
  if (s.includes('process') || s.includes('running') || s.includes('pending')) {
    return (
      <span className="inline-flex items-center gap-xs px-sm py-unit rounded-full bg-surface-container-high text-on-surface font-label-md text-label-md">
        <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
        Processing
      </span>
    );
  }
  if (s.includes('schedul') || s === 'scheduled') {
    return (
      <span className="inline-flex items-center gap-xs px-sm py-unit rounded-full bg-secondary/10 text-secondary font-label-md text-label-md">
        <span className="material-symbols-outlined text-[16px]">schedule</span>
        Scheduled
      </span>
    );
  }
  if (s.includes('fail') || s.includes('error')) {
    return (
      <span className="inline-flex items-center gap-xs px-sm py-unit rounded-full bg-error-container/30 text-error font-label-md text-label-md">
        <span className="material-symbols-outlined text-[16px]">error</span>
        Failed
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-xs px-sm py-unit rounded-full bg-surface-container text-on-surface-variant font-label-md text-label-md">
      {status || 'Unknown'}
    </span>
  );
}

function tableIcon(report) {
  const s = (report.status || '').toLowerCase();
  if (report.isScheduled || s.includes('schedul')) return 'calendar_today';
  if (s.includes('process') || s.includes('pending')) return 'hourglass_empty';
  return report.icon || 'description';
}

export default function DemandReportsPage() {
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [reports, setReports] = useState([]);
  const [reportFiles, setReportFiles] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState('all');
  const [selectedStatus, setSelectedStatus] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [openMenuReportId, setOpenMenuReportId] = useState(null);
  const [showFilterMenu, setShowFilterMenu] = useState(false);

  const [showReportModal, setShowReportModal] = useState(false);
  const [editingReportId, setEditingReportId] = useState(null);
  const [reportForm, setReportForm] = useState(emptyReportForm());
  const [savingReport, setSavingReport] = useState(false);

  const [pendingDeleteReport, setPendingDeleteReport] = useState(null);
  const [deletingReport, setDeletingReport] = useState(false);

  useEffect(() => {
    fetchReportsData();
  }, []);

  useEffect(() => {
    const handleClickOutside = () => setOpenMenuReportId(null);
    if (openMenuReportId != null) {
      window.addEventListener('click', handleClickOutside);
      return () => window.removeEventListener('click', handleClickOutside);
    }
  }, [openMenuReportId]);

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

  const fetchReportsData = async () => {
    setLoading(true);
    setApiError(null);
    try {
      const [reportsRes, filesRes] = await Promise.allSettled([
        api.getDemandReports(),
        api.getReportFiles(),
      ]);

      const failures = [];

      if (reportsRes.status === 'fulfilled') {
        setReports(Array.isArray(reportsRes.value.data) ? reportsRes.value.data : []);
      } else {
        setReports([]);
        failures.push(`Reports: ${getErrorMessage(reportsRes.reason)}`);
      }

      if (filesRes.status === 'fulfilled') {
        setReportFiles(Array.isArray(filesRes.value.data) ? filesRes.value.data : []);
      } else {
        setReportFiles([]);
        failures.push(`Files: ${getErrorMessage(filesRes.reason)}`);
      }

      if (failures.length > 0) {
        setApiError(
          reportsRes.status === 'rejected'
            ? getErrorMessage(reportsRes.reason, 'Unable to load /api/demand-reports. Retry when the backend is available.')
            : `Partial load — ${failures.join(' · ')}`
        );
      }
    } catch (err) {
      setApiError(getErrorMessage(err, 'Error connecting to http://localhost:8080/api'));
      setReports([]);
      setReportFiles([]);
    } finally {
      setLoading(false);
    }
  };

  const filteredReports = reports.filter((report) => {
    if (selectedFilter !== 'all' && resolveCategory(report.reportType) !== selectedFilter) {
      return false;
    }
    if (selectedStatus !== 'all') {
      const status = (report.status || (report.isScheduled ? 'scheduled' : 'ready')).toLowerCase();
      if (status !== selectedStatus.toLowerCase()) return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      const title = resolveTitle(report).toLowerCase();
      const summary = resolveSummary(report).toLowerCase();
      const type = (report.reportType || '').toLowerCase();
      const generatedBy = (report.generatedBy || '').toLowerCase();
      if (!title.includes(q) && !summary.includes(q) && !type.includes(q) && !generatedBy.includes(q)) {
        return false;
      }
    }
    return true;
  });

  const cardsToShow = filteredReports;
  const rowsToShow = filteredReports;

  const getFilesForReport = (reportId) =>
    reportFiles.filter((f) => f.reportId === reportId);

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
      flashError(getErrorMessage(err, `Unable to load report #${report.reportId} for editing.`));
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
        flashSuccess(`Report #${editingReportId} updated successfully.`);
      } else {
        const created = await api.createDemandReport(payload);
        flashSuccess(`Report #${created.data?.reportId || 'new'} created successfully.`);
      }

      setShowReportModal(false);
      await fetchReportsData();
    } catch (err) {
      flashError(getErrorMessage(err, editingReportId != null ? 'Failed to update report' : 'Failed to create report'));
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
      flashSuccess(`Report #${id} deleted successfully.`);
      await fetchReportsData();
    } catch (err) {
      flashError(getErrorMessage(err, 'Failed to delete report'));
    } finally {
      setDeletingReport(false);
    }
  };

  const handleExportCsv = (report) => {
    const files = getFilesForReport(report.reportId);
    const csvFile = files.find((f) => (f.format || '').toLowerCase() === 'csv');
    if (csvFile && csvFile.fileUrl) {
      window.open(csvFile.fileUrl, '_blank');
      flashSuccess('CSV download initiated.');
      return;
    }
    const headers = [
      'reportId', 'reportType', 'periodStart', 'periodEnd',
      'totalForecastedVolumeT', 'totalActualVolumeT', 'variancePct',
      'status', 'summary',
    ];
    const row = headers.map((h) => {
      const val = report[h] ?? '';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    }).join(',');
    const csv = [headers.join(','), row].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `report-${report.reportId || 'export'}-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    flashSuccess('CSV export downloaded.');
  };

  const handleExportPdf = (report) => {
    const files = getFilesForReport(report.reportId);
    const pdfFile = files.find((f) => (f.format || '').toLowerCase() === 'pdf');
    if (pdfFile && pdfFile.fileUrl) {
      window.open(pdfFile.fileUrl, '_blank');
      flashSuccess('PDF download initiated.');
      return;
    }
    flashError('No PDF file is available for this report yet.');
  };

  return (
    <div className="flex flex-col w-full h-full relative">
      <div className="px-margin-desktop py-xl">
        {apiError && (
          <div className="bg-secondary-container/20 border-l-4 border-secondary p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-secondary text-[24px]">wifi_off</span>
              <div>
                <p className="font-label-md text-label-md text-on-surface font-bold">API Connection Notice</p>
                <p className="font-body-sm text-body-sm text-on-surface-variant">{apiError}</p>
              </div>
            </div>
            <button
              onClick={fetchReportsData}
              className="px-md py-xs bg-secondary text-on-secondary font-label-md text-label-md rounded-lg hover:opacity-90 transition-opacity flex items-center gap-xs"
            >
              <span className="material-symbols-outlined text-[16px]">refresh</span> Retry
            </button>
          </div>
        )}

        {actionError && (
          <div className="bg-error-container border-l-4 border-error p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-error text-[24px]">error</span>
              <div>
                <p className="font-label-md text-label-md text-on-error-container font-bold">Action failed</p>
                <p className="font-body-sm text-body-sm text-on-error-container">{actionError}</p>
              </div>
            </div>
            <button onClick={() => setActionError(null)} className="text-on-error-container px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {actionSuccess && (
          <div className="bg-[#e8f5e9] border-l-4 border-[#2e7d32] p-md rounded-xl flex items-center justify-between shadow-sm mb-lg">
            <div className="flex items-center gap-md">
              <span className="material-symbols-outlined text-[#2e7d32] text-[24px]">check_circle</span>
              <p className="font-body-sm text-body-sm text-[#1b5e20]">{actionSuccess}</p>
            </div>
            <button onClick={() => setActionSuccess(null)} className="text-[#1b5e20] px-sm">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        )}

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-lg mb-xl">
          <div>
            <h2 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">Demand Reports</h2>
            <p className="font-body-lg text-body-lg text-on-surface-variant mt-sm max-w-2xl">
              Access and manage demand forecasting reports for Dried Mango exports from Burkina Faso to Germany. Monitor scenario analysis, regional demand, and revenue potential.
            </p>
          </div>
          <div className="flex items-center gap-md shrink-0">
            <button
              onClick={() => openCreateReport(true)}
              className="h-12 px-lg rounded-xl bg-surface-container border border-outline-variant flex items-center gap-sm hover:bg-surface-container-high transition-colors text-on-surface group"
            >
              <span className="material-symbols-outlined text-outline group-hover:text-primary transition-colors">calendar_month</span>
              <span className="font-label-md text-label-md uppercase tracking-wider">Schedule Report</span>
            </button>
            <button
              onClick={() => openCreateReport(false)}
              className="h-12 px-lg rounded-xl bg-primary text-on-primary flex items-center gap-sm hover:bg-on-primary-fixed-variant transition-colors shadow-md shadow-primary/20 hover:shadow-lg hover:shadow-primary/30 group"
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">add</span>
              <span className="font-label-md text-label-md uppercase tracking-wider">New Custom Report</span>
            </button>
          </div>
        </div>

        {/* Filter Tabs + Search */}
        <div className="flex flex-col lg:flex-row lg:items-center gap-md mb-xl">
          <div className="flex flex-wrap items-center gap-sm p-xs bg-surface-container-low rounded-xl inline-flex w-full sm:w-auto overflow-x-auto pb-sm sm:pb-xs">
            {FILTERS.map((filter) => (
              <button
                key={filter.id}
                onClick={() => setSelectedFilter(filter.id)}
                className={`px-lg py-sm rounded-lg font-label-md text-label-md uppercase tracking-wider flex items-center gap-xs shrink-0 transition-colors ${
                  selectedFilter === filter.id
                    ? 'bg-surface text-primary shadow-sm'
                    : 'hover:bg-surface-container text-on-surface-variant hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">{filter.icon}</span>
                {filter.label}
              </button>
            ))}
          </div>

          <div className="flex-1 flex items-center gap-md">
            <div className="relative flex-1 max-w-md">
              <span className="material-symbols-outlined text-on-surface-variant absolute left-md top-1/2 -translate-y-1/2 text-[20px]">search</span>
              <input
                type="text"
                placeholder="Search reports by name, type, summaryââ‚¬Â¦"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-12 pl-[44px] pr-md rounded-xl bg-surface-container border border-outline-variant/40 font-body-md focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
            <div className="relative shrink-0">
              <button
                type="button"
                onClick={() => setShowFilterMenu((value) => !value)}
                aria-expanded={showFilterMenu}
                className={`px-lg py-sm h-12 rounded-lg hover:bg-surface-container text-on-surface-variant hover:text-on-surface font-label-md text-label-md uppercase tracking-wider transition-colors flex items-center gap-xs border border-outline-variant/30 ${
                  showFilterMenu || selectedStatus !== 'all' ? 'bg-surface-container text-primary' : ''
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">filter_list</span>
                Filters
              </button>
              {showFilterMenu && (
                <div className="absolute right-0 top-14 z-20 w-48 rounded-lg border border-outline-variant/40 bg-surface-container-lowest p-xs shadow-lg">
                  <p className="px-sm py-xs font-label-md text-label-md uppercase text-on-surface-variant">Status</p>
                  {['all', ...STATUS_OPTIONS].map((status) => (
                    <button
                      key={status}
                      type="button"
                      onClick={() => {
                        setSelectedStatus(status);
                        setShowFilterMenu(false);
                      }}
                      className={`w-full rounded-md px-sm py-sm text-left font-body-sm text-body-sm hover:bg-surface-container ${
                        selectedStatus === status ? 'bg-primary/10 text-primary font-semibold' : 'text-on-surface'
                      }`}
                    >
                      {status === 'all' ? 'All statuses' : status}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Loading */}
        {loading && (
          <div className="mb-xl p-lg bg-surface-container-lowest rounded-xl text-center border border-outline-variant/20">
            <span className="material-symbols-outlined text-[32px] text-primary animate-spin">sync</span>
            <p className="font-body-sm text-body-sm text-on-surface-variant mt-sm">Loading demand reportsââ‚¬Â¦</p>
          </div>
        )}

        {/* Report Cards Grid */}
        {!loading && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-gutter mb-xxl">
            {cardsToShow.map((report, idx) => {
              const visuals = getCardVisuals(report, idx);
              const files = getFilesForReport(report.reportId);
              const hasCsv = visuals.csvEnabled || files.some((f) => (f.format || '').toLowerCase() === 'csv');
              return (
                <div
                  key={report.reportId || idx}
                  className="bg-surface-container-lowest rounded-2xl shadow-sm hover:shadow-md transition-shadow group relative overflow-hidden flex flex-col h-full"
                >
                  <div className={`absolute top-0 right-0 w-32 h-32 ${accentBlobClass(visuals.accent)} rounded-bl-full -z-10 transition-transform group-hover:scale-110`} />
                  <div className="p-lg flex-1 flex flex-col">
                    <div className="flex items-start justify-between mb-md">
                      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${iconToneClasses(visuals.iconTone)}`}>
                        <span className="material-symbols-outlined text-[24px]">{visuals.icon}</span>
                      </div>
                      <div className="relative">
                        {visuals.isAutoGen ? (
                          <span className="px-sm py-xs bg-surface-container-high rounded-full font-mono-label text-mono-label text-on-surface-variant flex items-center gap-xs">
                            <span className="w-1.5 h-1.5 rounded-full bg-primary" />
                            Auto-Gen
                          </span>
                        ) : (
                          <>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setOpenMenuReportId(openMenuReportId === report.reportId ? null : report.reportId);
                              }}
                              className="text-on-surface-variant hover:text-primary transition-colors p-xs"
                            >
                              <span className="material-symbols-outlined">more_vert</span>
                            </button>
                            {openMenuReportId === report.reportId && (
                              <div
                                onClick={(e) => e.stopPropagation()}
                                className="absolute right-0 top-full mt-xs bg-surface-container-lowest rounded-lg shadow-lg border border-outline-variant/30 py-xs z-20 min-w-[160px]"
                              >
                                <button
                                  onClick={() => {
                                    setOpenMenuReportId(null);
                                    openEditReport(report);
                                  }}
                                  className="w-full px-md py-sm text-left font-body-sm hover:bg-surface-container text-on-surface flex items-center gap-xs"
                                >
                                  <span className="material-symbols-outlined text-[18px] text-on-surface-variant">edit</span>
                                  Edit Report
                                </button>
                                <button
                                  onClick={() => {
                                    setOpenMenuReportId(null);
                                    setPendingDeleteReport(report);
                                  }}
                                  className="w-full px-md py-sm text-left font-body-sm hover:bg-error-container/30 text-error flex items-center gap-xs"
                                >
                                  <span className="material-symbols-outlined text-[18px]">delete</span>
                                  Delete Report
                                </button>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                    <h3 className="font-headline-sm text-headline-sm text-on-surface mb-xs">{resolveTitle(report)}</h3>
                    <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg line-clamp-2">
                      {resolveSummary(report)}
                    </p>
                    <div className="mt-auto">
                      <div className="flex items-center justify-between py-sm border-t border-surface-container mb-sm">
                        <span className="font-mono-label text-mono-label text-outline">Generated:</span>
                        <span className="font-body-sm text-body-sm text-on-surface">{formatPeriodLabel(report)}</span>
                      </div>
                      <div className="flex gap-xs">
                        <button
                          onClick={() => handleExportPdf(report)}
                          className="flex-1 h-10 bg-surface-container hover:bg-surface-container-high text-primary rounded-lg flex items-center justify-center gap-xs transition-colors group/btn"
                        >
                          <span className="material-symbols-outlined text-[20px] group-hover/btn:-translate-y-0.5 transition-transform">picture_as_pdf</span>
                          <span className="font-label-md text-label-md">PDF</span>
                        </button>
                        <button
                          onClick={() => handleExportCsv(report)}
                          disabled={!hasCsv}
                          className={`flex-1 h-10 bg-surface-container hover:bg-surface-container-high text-primary rounded-lg flex items-center justify-center gap-xs transition-colors group/btn ${
                            !hasCsv ? 'opacity-50 cursor-not-allowed' : ''
                          }`}
                        >
                          <span className={`material-symbols-outlined text-[20px] ${hasCsv ? 'group-hover/btn:-translate-y-0.5 transition-transform' : ''}`}>csv</span>
                          <span className="font-label-md text-label-md">CSV</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}

            {/* Create New Schedule Card */}
            <button
              onClick={() => openCreateReport(true)}
              className="bg-surface-container rounded-2xl border-2 border-dashed border-outline-variant/50 hover:border-primary/50 transition-colors flex flex-col items-center justify-center p-lg h-full cursor-pointer group text-center min-h-[280px]"
            >
              <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary mb-md group-hover:scale-110 transition-transform">
                <span className="material-symbols-outlined text-[32px]">add_task</span>
              </div>
              <h3 className="font-headline-sm text-headline-sm text-on-surface mb-xs">Create New Schedule</h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant">Automate report generation for your specific criteria.</p>
            </button>
          </div>
        )}

        {/* Recent Activity Table */}
        {!loading && (
          <div className="bg-surface-container-lowest rounded-2xl shadow-sm overflow-hidden flex flex-col">
            <div className="p-lg flex items-center justify-between bg-surface-container-lowest border-b border-surface-container-high">
              <h3 className="font-headline-md text-headline-md text-on-surface">Recent Activity</h3>
              <button
                type="button"
                onClick={() => {
                  setSelectedFilter('all');
                  setSelectedStatus('all');
                  setSearchQuery('');
                  flashSuccess('Showing full report history.');
                }}
                className="text-primary font-label-md text-label-md hover:underline flex items-center gap-xs"
              >
                View Full History <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </button>
            </div>
            <div className="overflow-x-auto w-full">
              <table className="w-full text-left min-w-[900px]">
                <thead>
                  <tr className="bg-surface font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">
                    <th className="py-md px-lg w-[35%]">Report Name</th>
                    <th className="py-md px-lg">Period</th>
                    <th className="py-md px-lg">Status</th>
                    <th className="py-md px-lg text-right pr-lg">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-surface-container font-body-md text-body-md text-on-surface">
                  {rowsToShow.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="py-xl px-lg text-center text-on-surface-variant">
                        No reports match this filter.
                      </td>
                    </tr>
                  ) : (
                    rowsToShow.map((report, idx) => {
                      const status = report.status || (report.isScheduled ? 'scheduled' : 'ready');
                      return (
                        <tr key={report.reportId || idx} className="hover:bg-surface-container-low/50 transition-colors group">
                          <td className="py-md px-lg">
                            <div className="flex items-center gap-md">
                              <div className="w-10 h-10 rounded-lg bg-surface-container flex items-center justify-center text-on-surface-variant">
                                <span className="material-symbols-outlined text-[20px]">{tableIcon(report)}</span>
                              </div>
                              <div>
                                <p className="font-medium">{resolveTitle(report)}</p>
                                <p className="font-body-sm text-body-sm text-on-surface-variant">
                                  {resolveSummary(report)}
                                </p>
                              </div>
                            </div>
                          </td>
                          <td className="py-md px-lg font-mono-label text-mono-label">
                            {formatPeriodLabel(report)}
                          </td>
                          <td className="py-md px-lg">
                            <StatusBadge status={status} />
                          </td>
                          <td className="py-md px-lg text-right pr-lg">
                            <div className="flex items-center justify-end gap-xs opacity-100 lg:opacity-0 group-hover:opacity-100 transition-opacity">
                              <button
                                onClick={() => handleExportPdf(report)}
                                title="Export PDF"
                                className="p-sm text-on-surface-variant hover:text-primary hover:bg-surface-container rounded-lg transition-colors"
                              >
                                <span className="material-symbols-outlined text-[20px]">picture_as_pdf</span>
                              </button>
                              <button
                                onClick={() => handleExportCsv(report)}
                                title="Export CSV"
                                className="p-sm text-on-surface-variant hover:text-primary hover:bg-surface-container rounded-lg transition-colors"
                              >
                                <span className="material-symbols-outlined text-[20px]">csv</span>
                              </button>
                              <button
                                onClick={() => openEditReport(report)}
                                title="Edit report"
                                className="p-sm text-on-surface-variant hover:text-primary hover:bg-surface-container rounded-lg transition-colors"
                              >
                                <span className="material-symbols-outlined text-[20px]">edit</span>
                              </button>
                              <button
                                onClick={() => setPendingDeleteReport(report)}
                                title="Delete report"
                                className="p-sm text-on-surface-variant hover:text-error hover:bg-error-container/30 rounded-lg transition-colors"
                              >
                                <span className="material-symbols-outlined text-[20px]">delete</span>
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Create / Edit Report Modal — POST /api/demand-reports  +  PUT /api/demand-reports/{id} */}
      {showReportModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-2xl p-xl relative max-h-[90vh] overflow-y-auto">
            <button
              type="button"
              onClick={() => setShowReportModal(false)}
              className="absolute top-md right-md text-on-surface-variant hover:text-primary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-xs">
              {editingReportId != null ? `Edit Report #${editingReportId}` : 'New Custom Report'}
            </h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              {editingReportId != null ? (
                <>Updates via <code className="text-primary">PUT /api/demand-reports/{editingReportId}</code></>
              ) : (
                <>Creates via <code className="text-primary">POST /api/demand-reports</code></>
              )}
            </p>
            <form onSubmit={handleSaveReport} className="flex flex-col gap-md">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Report Type</label>
                  <select
                    value={reportForm.reportType}
                    onChange={(e) => setReportForm((f) => ({ ...f, reportType: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    {REPORT_TYPES.map((rt) => (
                      <option key={rt} value={rt}>{rt}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Status</label>
                  <select
                    value={reportForm.status}
                    onChange={(e) => setReportForm((f) => ({ ...f, status: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  >
                    {STATUS_OPTIONS.map((st) => (
                      <option key={st} value={st}>{st}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Period Start *</label>
                  <input
                    required
                    type="date"
                    value={reportForm.periodStart}
                    onChange={(e) => setReportForm((f) => ({ ...f, periodStart: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Period End *</label>
                  <input
                    required
                    type="date"
                    value={reportForm.periodEnd}
                    onChange={(e) => setReportForm((f) => ({ ...f, periodEnd: e.target.value }))}
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-md">
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Forecast (t)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={reportForm.totalForecastedVolumeT}
                    onChange={(e) => setReportForm((f) => ({ ...f, totalForecastedVolumeT: e.target.value }))}
                    placeholder="e.g. 9000"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Actual (t)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={reportForm.totalActualVolumeT}
                    onChange={(e) => setReportForm((f) => ({ ...f, totalActualVolumeT: e.target.value }))}
                    placeholder="e.g. 8750"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
                <div>
                  <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Variance %</label>
                  <input
                    type="number"
                    step="0.01"
                    value={reportForm.variancePct}
                    onChange={(e) => setReportForm((f) => ({ ...f, variancePct: e.target.value }))}
                    placeholder="e.g. -2.8"
                    className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                  />
                </div>
              </div>

              <div className="p-md bg-surface-container-low rounded-lg border border-outline-variant/20">
                <label className="flex items-center gap-sm cursor-pointer select-none mb-md">
                  <input
                    type="checkbox"
                    checked={reportForm.isScheduled}
                    onChange={(e) => setReportForm((f) => ({ ...f, isScheduled: e.target.checked }))}
                    className="w-5 h-5 rounded accent-primary"
                  />
                  <span className="font-label-md text-label-md">Scheduled Report</span>
                </label>
                {reportForm.isScheduled && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
                    <div>
                      <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Next Run</label>
                      <input
                        type="datetime-local"
                        value={reportForm.nextRunAt}
                        onChange={(e) => setReportForm((f) => ({ ...f, nextRunAt: e.target.value }))}
                        className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                      />
                    </div>
                    <div>
                      <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Frequency</label>
                      <select
                        value={reportForm.scheduleFrequency}
                        onChange={(e) => setReportForm((f) => ({ ...f, scheduleFrequency: e.target.value }))}
                        className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                      >
                        <option value="">Selectââ‚¬Â¦</option>
                        {SCHEDULE_FREQUENCIES.map((f) => (
                          <option key={f} value={f}>{f}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Generated By</label>
                <input
                  type="text"
                  value={reportForm.generatedBy}
                  onChange={(e) => setReportForm((f) => ({ ...f, generatedBy: e.target.value }))}
                  placeholder="e.g. System, Admin, Analyst name"
                  className="w-full h-11 px-md rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm"
                />
              </div>

              <div>
                <label className="font-label-md text-label-md text-on-surface-variant uppercase mb-xs block">Summary</label>
                <textarea
                  rows={3}
                  value={reportForm.summary}
                  onChange={(e) => setReportForm((f) => ({ ...f, summary: e.target.value }))}
                  placeholder="Brief description of this report's contents and key insightsââ‚¬Â¦"
                  className="w-full px-md py-sm rounded-lg bg-surface-container border border-outline-variant/40 font-body-sm resize-none"
                />
              </div>

              <div className="flex gap-md mt-md">
                <button
                  type="button"
                  onClick={() => setShowReportModal(false)}
                  className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md text-on-surface hover:bg-surface-container-high transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingReport}
                  className="flex-1 h-11 rounded-xl bg-primary text-on-primary font-label-md text-label-md disabled:opacity-60 flex items-center justify-center gap-xs hover:bg-on-primary-fixed-variant transition-colors"
                >
                  {savingReport ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span> Savingââ‚¬Â¦
                    </>
                  ) : editingReportId != null ? (
                    <>
                      <span className="material-symbols-outlined text-[18px]">save</span> Save Changes
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">add</span> Create Report
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete confirmation — DELETE /api/demand-reports/{id} */}
      {pendingDeleteReport && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest rounded-xl shadow-xl w-full max-w-md p-xl">
            <h3 className="font-headline-lg text-headline-lg text-on-surface mb-sm">Delete report?</h3>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This calls <code className="text-primary">DELETE /api/demand-reports/{pendingDeleteReport.reportId}</code> for{' '}
              <strong>{resolveTitle(pendingDeleteReport)}</strong>. This cannot be undone.
            </p>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg line-clamp-3">
              {resolveSummary(pendingDeleteReport)}
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setPendingDeleteReport(null)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deletingReport}
                onClick={confirmDeleteReport}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md disabled:opacity-60 hover:opacity-90 transition-opacity"
              >
                {deletingReport ? 'Deletingââ‚¬Â¦' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
