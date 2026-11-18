import { ArrowRight, Calendar, FileSpreadsheet, FileText, Hourglass, Pencil, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button, Card, IconButton, Table } from '../../../../shared/ui';
import ReportStatusBadge from './ReportStatusBadge';
import { reportStatus, useReportText } from './reportUtils';

function rowIcon(report) {
  const status = (report.status || '').toLowerCase();
  if (report.isScheduled || status.includes('schedul')) return Calendar;
  if (status.includes('process') || status.includes('pending')) return Hourglass;
  return FileText;
}

/** Table of the filtered reports with their exports, edit and delete actions. */
function ReportActivityTable({ reports, onShowAll, onExportPdf, onExportCsv, onEdit, onDelete }) {
  const { t } = useTranslation('salesmarketing');
  const text = useReportText();

  const columns = [
    {
      key: 'name',
      header: t('reports.table.name'),
      className: 'w-1/3',
      cell: (report) => {
        const Icon = rowIcon(report);
        return (
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-surface-muted text-text-muted">
              <Icon size={20} aria-hidden="true" />
            </div>
            <div>
              <p className="font-medium">{text.title(report)}</p>
              <p className="text-sm font-normal text-text-muted">{text.summary(report)}</p>
            </div>
          </div>
        );
      },
    },
    { key: 'period', header: t('reports.table.period'), cell: (report) => text.period(report) },
    { key: 'status', header: t('reports.table.status'), cell: (report) => <ReportStatusBadge status={reportStatus(report)} /> },
  ];

  return (
    <Card
      title={t('reports.recentActivity')}
      actions={
        <Button variant="ghost" size="sm" onClick={onShowAll}>
          {t('reports.viewHistory')}
          <ArrowRight size={16} aria-hidden="true" />
        </Button>
      }
    >
      <Table
        caption={t('reports.recentActivity')}
        columns={columns}
        rows={reports}
        getRowKey={(report) => report.reportId}
        emptyMessage={t('reports.noMatch')}
        rowActions={(report) => (
          <div className="flex items-center justify-end gap-1">
            <IconButton icon={FileText} tone="neutral" label={t('reports.exportPdf')} onClick={() => onExportPdf(report)} />
            <IconButton icon={FileSpreadsheet} tone="neutral" label={t('reports.exportCsv')} onClick={() => onExportCsv(report)} />
            <IconButton icon={Pencil} tone="neutral" label={t('reports.editRow')} onClick={() => onEdit(report)} />
            <IconButton icon={Trash2} tone="danger" label={t('reports.deleteRow')} onClick={() => onDelete(report)} />
          </div>
        )}
      />
    </Card>
  );
}

export default ReportActivityTable;
