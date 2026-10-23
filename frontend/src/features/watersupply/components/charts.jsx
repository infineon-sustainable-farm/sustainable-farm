import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
  ReferenceArea,
  BarChart,
  Bar,
  RadialBarChart,
  RadialBar,
  PolarAngleAxis,
} from 'recharts'
import { useRef } from 'react'
import { C, exportChartAsPng, exportRowsAsCsv } from './chartUtils'

const tooltipStyle = {
  fontFamily: "'Inter', Arial, sans-serif",
  fontSize: '12px',
  borderRadius: '8px',
  border: '1px solid var(--ws-line, #e2e9e7)',
  background: '#fff',
  color: 'var(--ws-ink, #1c2b29)',
}

const axisTick = { fontSize: 11, fill: C.muted, fontFamily: 'Inter' }

const legendStyle = {
  fontFamily: "'Inter', Arial, sans-serif",
  fontSize: '11px',
  paddingTop: '4px',
}

/**
 * Formats a volume in litres with the matching unit (L, m³ above 10,000 L).
 * Charts must always carry a readable unit to be usable.
 * @param {number} liters
 * @param {number} [decimals]
 */
function formatLiters(liters, decimals = 0) {
  const value = Number(liters) || 0
  if (Math.abs(value) >= 10000) return `${(value / 1000).toFixed(1)} m³`
  return `${value.toFixed(decimals)} L`
}

/** Formats an amount in millimetres (rainfall, ET0). */
/** Compact unit suffix for axes (avoids long labels). */
function axisUnit(values, kind = 'liters') {
  const max = values.reduce((acc, v) => Math.max(acc, Math.abs(Number(v) || 0)), 0)
  if (kind === 'liters') return max >= 10000 ? { label: 'm³', divisor: 1000 } : { label: 'L', divisor: 1 }
  if (kind === 'mm') return { label: 'mm', divisor: 1 }
  return { label: '', divisor: 1 }
}

/** Formats an axis value according to the selected unit. */
function tickFormatter({ label, divisor }) {
  return (value) => {
    const scaled = (Number(value) || 0) / divisor
    return `${label === 'm³' ? scaled.toFixed(1) : Math.round(scaled)}`
  }
}

/**
 * Common shell: unit header, empty-state note, and PNG / CSV chart export.
 * The export is optional: it only appears when a file name is provided.
 *
 * @param {string} [exportName] - exported file name (without extension); omitted = no export.
 * @param {Array<Object>} [exportRows] - raw rows to export as CSV.
 * @param {Array<{ key: string, label: string }>} [exportColumns] - CSV columns.
 */
function ChartShell({ children, unit, note, exportName, exportRows, exportColumns }) {
  const shellRef = useRef(null)
  const canExport = Boolean(exportName)
  // The chart is exposed as an image with a label: a screen reader announces at least
  // what it represents (unit and note), instead of a silent SVG.
  const chartLabel = ['Chart', unit ? `in ${unit}` : null, note || null].filter(Boolean).join(' - ')
  return (
    <div className="ws-chart-shell">
      <div className="ws-chart-shell-top">
        {unit && <span className="ws-chart-unit">Unit: {unit}</span>}
        {canExport && (
          <div className="ws-chart-toolbar">
            <button
              type="button"
              aria-label="Export the chart as a PNG image"
              onClick={() => exportChartAsPng(shellRef.current, `${exportName}.png`)}
              title="Export the chart as a PNG image"
            >
              PNG
            </button>
            <button
              type="button"
              aria-label="Export the chart data as CSV"
              onClick={() => exportRowsAsCsv(exportRows || [], exportColumns || [], `${exportName}.csv`)}
              title="Export the chart data as CSV"
            >
              CSV
            </button>
          </div>
        )}
      </div>
      <div ref={shellRef} role="img" aria-label={chartLabel}>
        {children}
      </div>
      {note && <p className="ws-chart-note">{note}</p>}
    </div>
  )
}

/** CSV columns derived from the chart series (x axis + named series). */
function columnsOf(xKey, series = []) {
  return [{ key: xKey, label: xKey }, ...series.map((s) => ({ key: s.key, label: s.name || s.key }))]
}

/**
 * Area chart (time-series / volume data).
 *
 * Readability: a unit is always displayed, the legend only shows from two series,
 * and a reference series (dashed) lets measured values be compared against need.
 *
 * @param {Array} data - chart rows.
 * @param {string} xKey - X axis key.
 * @param {Array} series - [{ key, name, color, dashed?, fillArea? }]
 * @param {number} [height]
 * @param {'liters'|'mm'|'percent'} [unitKind] - unit of the values.
 * @param {Array} [referenceLines] - [{ value, label, color }] horizontal comparison lines.
 * @param {Object} [band] - { from, to, color, label } tolerance / target band.
 * @param {boolean} [showLegend]
 */
export function WsAreaChart({
  data,
  xKey,
  series,
  height = 260,
  unitKind = 'liters',
  referenceLines = [],
  band = null,
  showLegend,
  exportName,
}) {
  const unit = axisUnit((data || []).flatMap((row) => series.map((s) => row[s.key])), unitKind)
  const legend = showLegend === undefined ? series.length > 1 || referenceLines.length > 0 : showLegend
  return (
    <ChartShell
      unit={unitKind === 'liters' ? 'liters' : unitKind === 'mm' ? 'millimeters' : '%'}
      exportName={exportName}
      exportRows={data}
      exportColumns={columnsOf(xKey, series)}
    >
      <ResponsiveContainer width="100%" height={height}>
        <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          <defs>
            {series.map((s) => (
              <linearGradient key={s.key} id={`grad-${s.key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={s.color} stopOpacity={0.28} />
                <stop offset="100%" stopColor={s.color} stopOpacity={0.02} />
              </linearGradient>
            ))}
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={axisTick} axisLine={false} tickLine={false} />
          <YAxis
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter(unit)}
            label={{ value: unit.label, angle: 0, position: 'insideTopLeft', offset: 8, fill: C.muted, fontSize: 10 }}
          />
          <Tooltip contentStyle={tooltipStyle} />
          {legend && <Legend wrapperStyle={legendStyle} iconType="plainline" />}
          {band && (
            <ReferenceArea
              y1={band.from}
              y2={band.to}
              fill={band.color || C.green}
              fillOpacity={0.08}
              stroke="none"
              label={{ value: band.label, position: 'insideTopRight', fill: C.muted, fontSize: 10 }}
            />
          )}
          {referenceLines.map((ref) => (
            <ReferenceLine
              key={ref.label || ref.value}
              y={ref.value}
              stroke={ref.color || C.muted}
              strokeDasharray="5 4"
              strokeWidth={1.5}
              label={{ value: ref.label, position: 'insideBottomRight', fill: ref.color || C.muted, fontSize: 10 }}
            />
          ))}
          {series.map((s) => (
            <Area
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.name}
              stroke={s.color}
              fill={s.fillArea === false ? 'transparent' : `url(#grad-${s.key})`}
              strokeWidth={2}
              strokeDasharray={s.dashed ? '6 4' : undefined}
              dot={false}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Bar chart (comparison / breakdown), with support for stacked bars
 * and a reference line (alert threshold, quota).
 *
 * @param {Array} data - [{name, ...}]
 * @param {string} xKey
 * @param {Array} bars - [{key, name, color, stackId?}]
 * @param {number} [height]
 * @param {'liters'|'mm'|'percent'} [unitKind]
 * @param {Object} [referenceLine] - { value, label, color }
 */
export function WsBarChart({
  data,
  xKey,
  bars,
  height = 260,
  unitKind = 'liters',
  referenceLine = null,
  exportName,
}) {
  const unit = axisUnit((data || []).flatMap((row) => bars.map((b) => row[b.key])), unitKind)
  return (
    <ChartShell
      unit={unitKind === 'liters' ? 'liters' : unitKind === 'mm' ? 'millimeters' : '%'}
      exportName={exportName}
      exportRows={data}
      exportColumns={columnsOf(xKey, bars)}
    >
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }} barGap={4}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={axisTick} axisLine={false} tickLine={false} />
          <YAxis
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter(unit)}
            label={{ value: unit.label, angle: 0, position: 'insideTopLeft', offset: 8, fill: C.muted, fontSize: 10 }}
          />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgba(10,130,118,.05)' }} />
          {bars.length > 1 && <Legend wrapperStyle={legendStyle} iconType="square" />}
          {referenceLine && (
            <ReferenceLine
              y={referenceLine.value}
              stroke={referenceLine.color || C.orange}
              strokeDasharray="5 4"
              label={{ value: referenceLine.label, position: 'insideTopRight', fill: referenceLine.color || C.orange, fontSize: 10 }}
            />
          )}
          {bars.map((b) => (
            <Bar
              key={b.key}
              dataKey={b.key}
              name={b.name}
              fill={b.color}
              stackId={b.stackId}
              radius={b.stackId ? undefined : [4, 4, 0, 0]}
              maxBarSize={28}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Radial 0-100 gauge (tank, volume, %, etc.).
 * @param {number} value - value between 0 and max.
 * @param {number} [max]
 * @param {string} [color]
 */
export function WsRadialGauge({ value = 0, max = 100, color = C.primary, height = 220 }) {
  const pct = Math.max(0, Math.min(100, (value / (max || 1)) * 100))
  const data = [{ name: 'level', value: pct, fill: color }]
  return (
    <div style={{ position: 'relative', width: '100%', maxWidth: 220, margin: '0 auto' }}>
      <ResponsiveContainer width="100%" height={height}>
        <RadialBarChart cx="50%" cy="50%" innerRadius="72%" outerRadius="100%" barSize={16} data={data} startAngle={90} endAngle={-270}>
          <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
          <RadialBar background={{ fill: C.line }} dataKey="value" cornerRadius={10} angleAxisId={0} />
        </RadialBarChart>
      </ResponsiveContainer>
      <div
        style={{
          position: 'absolute',
          inset: 0,
          display: 'grid',
          placeItems: 'center',
          fontFamily: "'Montserrat', Arial, sans-serif",
          fontWeight: 700,
          fontSize: '28px',
          color: 'var(--ws-ink, #1c2b29)',
          pointerEvents: 'none',
        }}
      >
        {Math.round(pct)}%
      </div>
    </div>
  )
}


const primaryRgb = '10,130,118'

/**
 * "Bullet" chart: a value compared to a target, on a zoned scale.
 * Used for consumption quotas (monthly target vs actual).
 *
 * @param {number} value
 * @param {number} target
 * @param {number} max
 * @param {Array} [zones] - [{ upTo, color }] background zones.
 * @param {string} [unit]
 */
export function WsBulletChart({ value = 0, target = 0, max = 100, zones = [], unit = 'L' }) {
  const safeMax = max || 100
  const pct = (v) => Math.max(0, Math.min(100, (Number(v) / safeMax) * 100))
  const overTarget = Number(value) > Number(target)
  return (
    <div className="ws-bullet">
      <div className="ws-bullet-track">
        {zones.map((zone, i) => (
          <span key={i} className="ws-bullet-zone" style={{ width: `${pct(zone.upTo)}%`, background: zone.color }} />
        ))}
        <span
          className="ws-bullet-value"
          style={{ width: `${pct(value)}%`, background: overTarget ? 'rgb(198,40,40)' : primaryRgb ? `rgb(${primaryRgb})` : undefined }}
        />
        <span className="ws-bullet-target" style={{ left: `${pct(target)}%` }} title={`Target ${formatLiters(target)}`} />
      </div>
      <div className="ws-bullet-caption">
        <strong>{formatLiters(value)}</strong>
        {` / target ${formatLiters(target)}${unit === '%' ? ' %' : ''}`}
      </div>
    </div>
  )
}
