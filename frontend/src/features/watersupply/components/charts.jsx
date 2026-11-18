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
import { useTranslation } from 'react-i18next'
import { Button } from '../../../shared/ui'
import { useWaterFormat } from '../format'
import { C, exportChartAsPng, exportRowsAsCsv } from './chartUtils'

// Colours and sizes from the tokens of src/index.css, as the machinery fuel chart. The axis ticks
// are SVG attributes: their font size is the text-xs value as a number.
const TEXT_XS = 12
const AXIS_TICK = { fill: C.muted, fontSize: TEXT_XS }
const AXIS_LABEL = { fill: C.muted, fontSize: TEXT_XS }
const TOOLTIP_STYLE = {
  borderRadius: 'var(--radius-tile)',
  border: 'thin solid var(--color-border-divider)',
  fontSize: 'var(--text-xs)',
}
const LEGEND_STYLE = { fontSize: 'var(--text-xs)' }
const AREA_OPACITY = 0.15
const BAND_OPACITY = 0.08
const BAR_RADIUS = [4, 4, 0, 0]
const BAR_MAX_SIZE = 28

/** Compact unit of an axis: m³ above 10,000 L. */
function axisUnit(values, kind = 'liters') {
  const max = values.reduce((acc, v) => Math.max(acc, Math.abs(Number(v) || 0)), 0)
  if (kind === 'liters') return max >= 10000 ? { key: 'cubicMeters', divisor: 1000 } : { key: 'liters', divisor: 1 }
  if (kind === 'mm') return { key: 'mm', divisor: 1 }
  return { key: 'percent', divisor: 1 }
}

/** Unit name shown above a chart. */
const UNIT_NAMES = { liters: 'liters', mm: 'millimeters', percent: 'percent' }

/**
 * Common frame: unit line and, when a file name is given, PNG and CSV export of the chart.
 *
 * @param {string} [exportName] - exported file name (without extension); omitted = no export.
 * @param {Array<Object>} [exportRows] - raw rows exported as CSV.
 * @param {Array<{ key: string, label: string }>} [exportColumns] - CSV columns.
 */
function ChartShell({ children, unitKind, exportName, exportRows, exportColumns }) {
  const { t } = useTranslation('watersupply')
  const shellRef = useRef(null)
  const unit = t(`charts.units.${UNIT_NAMES[unitKind] ?? UNIT_NAMES.liters}`)
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="text-xs text-text-muted">{t('charts.unit', { unit })}</span>
        {exportName && (
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="secondary"
              aria-label={t('charts.exportPng')}
              title={t('charts.exportPng')}
              onClick={() => exportChartAsPng(shellRef.current, `${exportName}.png`)}
            >
              {t('charts.png')}
            </Button>
            <Button
              size="sm"
              variant="secondary"
              aria-label={t('charts.exportCsv')}
              title={t('charts.exportCsv')}
              onClick={() => exportRowsAsCsv(exportRows || [], exportColumns || [], `${exportName}.csv`)}
            >
              {t('charts.csv')}
            </Button>
          </div>
        )}
      </div>
      {/* The chart is exposed as an image named by its unit, instead of a silent SVG. */}
      <div ref={shellRef} role="img" aria-label={t('charts.label', { unit })} className="h-64 w-full">
        {children}
      </div>
    </div>
  )
}

/** CSV columns derived from the chart series (x axis + named series). */
function columnsOf(xKey, series = []) {
  return [{ key: xKey, label: xKey }, ...series.map((s) => ({ key: s.key, label: s.name || s.key }))]
}

function useTickFormatter(unit) {
  const format = useWaterFormat()
  return (value) => {
    const scaled = (Number(value) || 0) / unit.divisor
    return unit.key === 'cubicMeters' ? format.fixed(scaled, 1) : format.fixed(scaled, 0)
  }
}

/**
 * Area chart (time series, volumes). A unit is always shown, the legend only from two series,
 * and dashed reference lines compare measured values with a need or a capacity.
 *
 * @param {Array} data - chart rows.
 * @param {string} xKey - X axis key.
 * @param {Array} series - [{ key, name, color, dashed?, fillArea? }]
 * @param {'liters'|'mm'|'percent'} [unitKind]
 * @param {Array} [referenceLines] - [{ value, label, color }]
 * @param {Object} [band] - { from, to, color, label } tolerance or target band.
 * @param {boolean} [showLegend]
 */
export function WsAreaChart({ data, xKey, series, unitKind = 'liters', referenceLines = [], band = null, showLegend, exportName }) {
  const { t } = useTranslation('watersupply')
  const unit = axisUnit((data || []).flatMap((row) => series.map((s) => row[s.key])), unitKind)
  const tickFormatter = useTickFormatter(unit)
  const legend = showLegend === undefined ? series.length > 1 || referenceLines.length > 0 : showLegend
  return (
    <ChartShell unitKind={unitKind} exportName={exportName} exportRows={data} exportColumns={columnsOf(xKey, series)}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={AXIS_TICK} axisLine={false} tickLine={false} />
          <YAxis
            tick={AXIS_TICK}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter}
            label={{ value: t(`charts.axis.${unit.key}`), angle: 0, position: 'insideTopLeft', offset: 8, ...AXIS_LABEL }}
          />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
          {legend && <Legend wrapperStyle={LEGEND_STYLE} iconType="plainline" />}
          {band && (
            <ReferenceArea
              y1={band.from}
              y2={band.to}
              fill={band.color || C.green}
              fillOpacity={BAND_OPACITY}
              stroke="none"
              label={{ value: band.label, position: 'insideTopRight', ...AXIS_LABEL }}
            />
          )}
          {referenceLines.map((ref) => (
            <ReferenceLine
              key={ref.label || ref.value}
              y={ref.value}
              stroke={ref.color || C.muted}
              strokeDasharray="5 4"
              strokeWidth={1.5}
              label={{ value: ref.label, position: 'insideBottomRight', fill: ref.color || C.muted, fontSize: TEXT_XS }}
            />
          ))}
          {series.map((s) => (
            <Area
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.name}
              stroke={s.color}
              fill={s.fillArea === false ? 'transparent' : s.color}
              fillOpacity={AREA_OPACITY}
              strokeWidth={2}
              strokeDasharray={s.dashed ? '6 4' : undefined}
              dot={false}
              isAnimationActive={false}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Bar chart (comparison, breakdown), stacked bars and a reference line (threshold, quota).
 *
 * @param {Array} data - [{ name, ... }]
 * @param {string} xKey
 * @param {Array} bars - [{ key, name, color, stackId? }]
 * @param {'liters'|'mm'|'percent'} [unitKind]
 * @param {Object} [referenceLine] - { value, label, color }
 */
export function WsBarChart({ data, xKey, bars, unitKind = 'liters', referenceLine = null, exportName }) {
  const { t } = useTranslation('watersupply')
  const unit = axisUnit((data || []).flatMap((row) => bars.map((b) => row[b.key])), unitKind)
  const tickFormatter = useTickFormatter(unit)
  return (
    <ChartShell unitKind={unitKind} exportName={exportName} exportRows={data} exportColumns={columnsOf(xKey, bars)}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barGap={4}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={AXIS_TICK} axisLine={false} tickLine={false} />
          <YAxis
            tick={AXIS_TICK}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter}
            label={{ value: t(`charts.axis.${unit.key}`), angle: 0, position: 'insideTopLeft', offset: 8, ...AXIS_LABEL }}
          />
          <Tooltip contentStyle={TOOLTIP_STYLE} cursor={{ fill: C.line }} />
          {bars.length > 1 && <Legend wrapperStyle={LEGEND_STYLE} iconType="square" />}
          {referenceLine && (
            <ReferenceLine
              y={referenceLine.value}
              stroke={referenceLine.color || C.orange}
              strokeDasharray="5 4"
              label={{ value: referenceLine.label, position: 'insideTopRight', fill: referenceLine.color || C.orange, fontSize: TEXT_XS }}
            />
          )}
          {bars.map((b) => (
            <Bar
              key={b.key}
              dataKey={b.key}
              name={b.name}
              fill={b.color}
              stackId={b.stackId}
              radius={b.stackId ? undefined : BAR_RADIUS}
              maxBarSize={BAR_MAX_SIZE}
              isAnimationActive={false}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Radial 0-100 gauge (tank level, reservoir), the percentage in its centre.
 * @param {number} value - between 0 and max.
 * @param {number} [max]
 * @param {string} [color] - chart colour (C.*).
 */
export function WsRadialGauge({ value = 0, max = 100, color = C.primary }) {
  const format = useWaterFormat()
  const pct = Math.max(0, Math.min(100, (value / (max || 1)) * 100))
  const data = [{ name: 'level', value: pct, fill: color }]
  return (
    <div className="relative mx-auto h-56 w-full max-w-56">
      <ResponsiveContainer width="100%" height="100%">
        <RadialBarChart cx="50%" cy="50%" innerRadius="72%" outerRadius="100%" barSize={16} data={data} startAngle={90} endAngle={-270}>
          <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
          <RadialBar background={{ fill: C.line }} dataKey="value" cornerRadius={10} angleAxisId={0} isAnimationActive={false} />
        </RadialBarChart>
      </ResponsiveContainer>
      <div className="pointer-events-none absolute inset-0 grid place-items-center text-3xl font-bold">
        {format.percent(Math.round(pct))}
      </div>
    </div>
  )
}

// Background zones of the bullet chart, from the tone tokens.
const ZONES = { success: 'bg-success-bg', warning: 'bg-warning-bg', error: 'bg-error-bg' }

/**
 * "Bullet" chart: a value compared with a target on a zoned scale (monthly quota vs actual).
 * The bar widths and the target position are percentages of the scale, set as inline styles.
 *
 * @param {number} value
 * @param {number} target
 * @param {number} max
 * @param {Array} [zones] - [{ upTo, tone: 'success'|'warning'|'error' }] background zones.
 */
export function WsBulletChart({ value = 0, target = 0, max = 100, zones = [] }) {
  const { t } = useTranslation('watersupply')
  const format = useWaterFormat()
  const safeMax = max || 100
  const pct = (v) => Math.max(0, Math.min(100, (Number(v) / safeMax) * 100))
  const overTarget = Number(value) > Number(target)
  const targetLabel = t('charts.target', { value: format.volume(target) })
  return (
    <div className="flex flex-col gap-2">
      <div className="relative h-4 overflow-hidden rounded-pill bg-neutral-bg">
        {/* Widest zone first: each narrower zone is painted over it, from the left edge. */}
        {[...zones].sort((a, b) => b.upTo - a.upTo).map((zone) => (
          <span
            key={`${zone.tone}-${zone.upTo}`}
            className={`absolute inset-y-0 left-0 ${ZONES[zone.tone] ?? ZONES.success}`}
            style={{ width: `${pct(zone.upTo)}%` }}
          />
        ))}
        <span
          className={`absolute top-1 bottom-1 left-0 rounded-pill ${overTarget ? 'bg-danger' : 'bg-primary'}`}
          style={{ width: `${pct(value)}%` }}
        />
        <span
          className="absolute inset-y-0 w-0.5 bg-text"
          style={{ left: `${pct(target)}%` }}
          title={targetLabel}
        />
      </div>
      <p className="text-xs text-text-muted">
        <strong className="font-semibold text-text">{format.volume(value)}</strong>
        {` ${t('charts.ofTarget', { value: format.volume(target) })}`}
      </p>
    </div>
  )
}
