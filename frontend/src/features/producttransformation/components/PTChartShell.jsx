/**
 * Product Transformation - chart shell.
 *
 * One card around every dashboard chart: a title, a caption, the unit, the
 * optional "Demo data" badge, and PNG / CSV export. The Water Supply module's
 * `ChartShell` is the reference for the behaviour; this is the PT-local copy
 * so the module does not import across feature slices.
 *
 * Export rules:
 *  - CSV is offered when `exportName`, `exportRows` and `exportColumns` are set.
 *  - PNG is offered only for single-chart cards (`allowPng` defaults to true);
 *    a card that renders two SVGs sets it to false, because the PNG helper can
 *    only capture the first one honestly.
 */

import { useRef } from 'react';
import { CARD_CAPTION, CARD_FRAME, CARD_TITLE } from '../styles/ptStyles';
import { exportChartAsPng, exportRowsAsCsv } from '../utils/chartExport';

const EXPORT_BUTTON =
  'inline-flex h-8 items-center rounded-md border border-gray-200 bg-white px-2 text-xs font-semibold text-gray-600 hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary';

export default function PTChartShell({
  title,
  subtitle,
  unit,
  note,
  exportName,
  exportRows,
  exportColumns,
  allowPng = true,
  demo = false,
  empty = false,
  className = '',
  animationDelay,
  children,
}) {
  const shellRef = useRef(null);
  const canExportCsv = Boolean(exportName && exportRows && exportColumns && !empty);
  const canExportPng = Boolean(exportName && allowPng && !empty);

  const chartLabel = ['Chart', unit ? `in ${unit}` : null, note || subtitle || null]
    .filter(Boolean)
    .join(' - ');

  return (
    <section
      className={`${CARD_FRAME} p-5 ${className}`}
      style={animationDelay ? { animationDelay } : undefined}
    >
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className={CARD_TITLE}>{title}</h2>
            {demo ? (
              <span className="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-800">
                Demo data
              </span>
            ) : null}
          </div>
          {subtitle ? <p className="mt-1 text-sm text-gray-500">{subtitle}</p> : null}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {unit ? (
            <span className="inline-flex h-8 items-center rounded-md bg-gray-50 px-2 text-xs font-medium text-gray-600">
              Unit: {unit}
            </span>
          ) : null}
          {canExportPng ? (
            <button
              type="button"
              aria-label="Export the chart as a PNG image"
              title="Export the chart as a PNG image"
              className={EXPORT_BUTTON}
              onClick={() => exportChartAsPng(shellRef.current, `${exportName}.png`)}
            >
              PNG
            </button>
          ) : null}
          {canExportCsv ? (
            <button
              type="button"
              aria-label="Export the chart data as CSV"
              title="Export the chart data as CSV"
              className={EXPORT_BUTTON}
              onClick={() => exportRowsAsCsv(exportRows, exportColumns, `${exportName}.csv`)}
            >
              CSV
            </button>
          ) : null}
        </div>
      </div>

      <div ref={shellRef} role={empty ? 'group' : 'img'} aria-label={chartLabel}>
        {children}
      </div>

      {note ? <p className={`mt-3 ${CARD_CAPTION}`}>{note}</p> : null}
    </section>
  );
}
