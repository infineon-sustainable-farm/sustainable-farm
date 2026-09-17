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
  ComposedChart,
  Line,
  Cell,
  RadialBarChart,
  RadialBar,
  PolarAngleAxis,
  ScatterChart,
  Scatter,
  ZAxis,
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
 * Formate un volume en litres avec l'unité adaptée (L, m³ au-delà de 10 000 L).
 * Les graphiques doivent toujours porter une unité lisible pour être exploitables.
 * @param {number} liters
 * @param {number} [decimals]
 */
function formatLiters(liters, decimals = 0) {
  const value = Number(liters) || 0
  if (Math.abs(value) >= 10000) return `${(value / 1000).toFixed(1)} m³`
  return `${value.toFixed(decimals)} L`
}

/** Formate une quantité en millimètres (pluviométrie, ET0). */
/** Suffixe d'unité compact pour les axes (évite les libellés longs). */
function axisUnit(values, kind = 'liters') {
  const max = values.reduce((acc, v) => Math.max(acc, Math.abs(Number(v) || 0)), 0)
  if (kind === 'liters') return max >= 10000 ? { label: 'm³', divisor: 1000 } : { label: 'L', divisor: 1 }
  if (kind === 'mm') return { label: 'mm', divisor: 1 }
  return { label: '', divisor: 1 }
}

/** Formate une valeur d'axe selon l'unité retenue. */
function tickFormatter({ label, divisor }) {
  return (value) => {
    const scaled = (Number(value) || 0) / divisor
    return `${label === 'm³' ? scaled.toFixed(1) : Math.round(scaled)}`
  }
}

/**
 * Enveloppe commune : titre d'unité, note d'état vide, et export PNG / CSV du graphique.
 * L'export est optionnel : il n'apparaît que si un nom de fichier est fourni.
 *
 * @param {string} [exportName] - nom du fichier exporté (sans extension) ; absent = pas d'export.
 * @param {Array<Object>} [exportRows] - données brutes à exporter en CSV.
 * @param {Array<{ key: string, label: string }>} [exportColumns] - colonnes du CSV.
 */
function ChartShell({ children, unit, note, exportName, exportRows, exportColumns }) {
  const shellRef = useRef(null)
  const canExport = Boolean(exportName)
  return (
    <div className="ws-chart-shell">
      <div className="ws-chart-shell-top">
        {unit && <span className="ws-chart-unit">Unité : {unit}</span>}
        {canExport && (
          <div className="ws-chart-toolbar">
            <button
              type="button"
              onClick={() => exportChartAsPng(shellRef.current, `${exportName}.png`)}
              title="Exporter le graphique en image PNG"
            >
              PNG
            </button>
            <button
              type="button"
              onClick={() => exportRowsAsCsv(exportRows || [], exportColumns || [], `${exportName}.csv`)}
              title="Exporter les données du graphique en CSV"
            >
              CSV
            </button>
          </div>
        )}
      </div>
      <div ref={shellRef}>
        {children}
      </div>
      {note && <p className="ws-chart-note">{note}</p>}
    </div>
  )
}

/** Colonnes CSV dérivées des séries du graphique (abscisse + séries nommées). */
function columnsOf(xKey, series = []) {
  return [{ key: xKey, label: xKey }, ...series.map((s) => ({ key: s.key, label: s.name || s.key }))]
}

/**
 * Graphique en aires (données de séries temporelles / volumes).
 *
 * Lisibilité : une unité est toujours affichée, la légende n'apparaît qu'à partir de
 * deux séries, et une série de référence (pointillés) permet de comparer le mesuré au besoin.
 *
 * @param {Array} data - lignes du graphique.
 * @param {string} xKey - clé axe X.
 * @param {Array} series - [{ key, name, color, dashed?, fillArea? }]
 * @param {number} [height]
 * @param {'liters'|'mm'|'percent'} [unitKind] - unité des valeurs.
 * @param {Array} [referenceLines] - [{ value, label, color }] lignes horizontales de comparaison.
 * @param {Object} [band] - { from, to, color, label } bande de tolérance / objectif.
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
      unit={unitKind === 'liters' ? 'litres' : unitKind === 'mm' ? 'millimètres' : '%'}
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
 * Graphique en barres (comparaison / répartition), avec support des barres empilées
 * et d'une ligne de référence (seuil d'alerte, quota).
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
      unit={unitKind === 'liters' ? 'litres' : unitKind === 'mm' ? 'millimètres' : '%'}
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
 * Jauge radiale 0-100 (réservoir, volume, %, etc.).
 * @param {number} value - valeur entre 0 et max.
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

/**
 * Aires empilées : répartition d'un volume total (par source, par zone...).
 * Le total reste lisible tout en montrant la contribution de chaque série.
 *
 * @param {Array} data
 * @param {string} xKey
 * @param {Array} series - [{ key, name, color }]
 * @param {number} [height]
 * @param {'liters'|'mm'|'percent'} [unitKind]
 */
export function WsStackedAreaChart({ data, xKey, series = [], height = 260, unitKind = 'liters', exportName }) {
  const unit = axisUnit((data || []).flatMap((row) => series.map((s) => row[s.key])), unitKind)
  return (
    <ChartShell
      unit={unitKind === 'liters' ? 'litres' : unitKind === 'mm' ? 'millimètres' : '%'}
      exportName={exportName}
      exportRows={data}
      exportColumns={columnsOf(xKey, series)}
    >
      <ResponsiveContainer width="100%" height={height}>
        <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
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
          {series.length > 1 && <Legend wrapperStyle={legendStyle} iconType="square" />}
          {series.map((s) => (
            <Area
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.name}
              stackId="stack"
              stroke={s.color}
              fill={s.color}
              fillOpacity={0.55}
              strokeWidth={1.5}
              dot={false}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Graphique combiné : la pluie (barres, mm) face aux volumes irrigués (ligne, litres).
 * C'est la lecture qui rend visible l'économie d'eau : on voit les journées où la pluie
 * couvre le besoin. Deux axes sont nécessaires car les unités diffèrent.
 *
 * @param {Array} data
 * @param {string} xKey
 * @param {Array} bars - [{ key, name, color }] valeurs en mm.
 * @param {Array} lines - [{ key, name, color, dashed? }] valeurs en litres.
 * @param {number} [height]
 */
export function WsComboChart({ data, xKey, bars = [], lines = [], height = 280, exportName }) {
  const barUnit = axisUnit((data || []).flatMap((row) => bars.map((b) => row[b.key])), 'mm')
  const lineUnit = axisUnit((data || []).flatMap((row) => lines.map((l) => row[l.key])), 'liters')
  return (
    <ChartShell
      unit="pluie en millimètres (barres) · volumes en litres (ligne)"
      exportName={exportName}
      exportRows={data}
      exportColumns={columnsOf(xKey, [...bars, ...lines])}
    >
      <ResponsiveContainer width="100%" height={height}>
        <ComposedChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={axisTick} axisLine={false} tickLine={false} />
          <YAxis
            yAxisId="bars"
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={42}
            tickFormatter={tickFormatter(barUnit)}
          />
          <YAxis
            yAxisId="lines"
            orientation="right"
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter(lineUnit)}
          />
          <Tooltip contentStyle={tooltipStyle} />
          {(bars.length + lines.length) > 1 && <Legend wrapperStyle={legendStyle} />}
          {bars.map((b) => (
            <Bar key={b.key} yAxisId="bars" dataKey={b.key} name={b.name} fill={b.color} radius={[4, 4, 0, 0]} maxBarSize={26} />
          ))}
          {lines.map((l) => (
            <Line
              key={l.key}
              yAxisId="lines"
              type="monotone"
              dataKey={l.key}
              name={l.name}
              stroke={l.color}
              strokeWidth={2}
              strokeDasharray={l.dashed ? '6 4' : undefined}
              dot={false}
            />
          ))}
        </ComposedChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Bilan en cascade (waterfall) : montre comment des entrées et des sorties
 * construisent un résultat. C'est la lecture qui rend visibles les pertes,
 * car chaque poste est identifiable au lieu d'être noyé dans un total.
 *
 * @param {Array} data - [{ name, value, kind: 'in'|'out'|'total', color? }]
 * @param {number} [height]
 * @param {'liters'|'percent'} [unitKind]
 */
export function WsWaterfallChart({ data, xKey = 'name', height = 280, unitKind = 'liters', exportName }) {
  const rows = []
  let running = 0
  for (const item of data || []) {
    const value = Number(item.value) || 0
    if (item.kind === 'total') {
      rows.push({ name: item.name, base: 0, delta: value, kind: 'total', fill: item.color || C.primary })
      running = value
      continue
    }
    const signed = item.kind === 'out' ? -Math.abs(value) : Math.abs(value)
    const next = running + signed
    rows.push({
      name: item.name,
      base: Math.min(running, next),
      delta: Math.abs(signed),
      kind: item.kind,
      fill: item.color || (item.kind === 'out' ? C.orange : C.green),
    })
    running = next
  }
  const unit = axisUnit(rows.map((row) => row.base + row.delta), unitKind)
  return (
    <ChartShell
      unit={unitKind === 'liters' ? 'litres' : '%'}
      exportName={exportName}
      exportRows={rows}
      exportColumns={[{ key: 'name', label: 'poste' }, { key: 'delta', label: 'volume' }, { key: 'kind', label: 'type' }]}
    >
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={rows} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} vertical={false} />
          <XAxis dataKey={xKey} tick={axisTick} axisLine={false} tickLine={false} interval={0} />
          <YAxis
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={48}
            tickFormatter={tickFormatter(unit)}
            label={{ value: unit.label, angle: 0, position: 'insideTopLeft', offset: 8, fill: C.muted, fontSize: 10 }}
          />
          <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'rgba(10,130,118,.05)' }} />
          <Bar dataKey="base" stackId="wf" fill="transparent" isAnimationActive={false} />
          <Bar dataKey="delta" stackId="wf" name="Volume" radius={[4, 4, 0, 0]} maxBarSize={40}>
            {rows.map((row) => (
              <Cell key={row.name} fill={row.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

/**
 * Courbe des économies cumulées : le volume d'eau économisé s'additionne dans le temps
 * et une ligne cible rappelle l'objectif. C'est la preuve chiffrée de la performance.
 *
 * @param {Array} data - [{ label, value }]
 * @param {string} dataKey
 * @param {number} [target] - objectif cumulé à afficher en pointillés.
 * @param {number} [height]
 */
export function WsCumulativeChart({ data, xKey = 'label', dataKey = 'cumulative_liters', target = null, height = 240 }) {
  return (
    <WsAreaChart
      data={data}
      xKey={xKey}
      height={height}
      unitKind="liters"
      series={[{ key: dataKey, name: "Économie cumulée", color: C.green }]}
      referenceLines={target ? [{ value: target, label: `Objectif ${formatLiters(target)}`, color: C.orange }] : []}
    />
  )
}

/**
 * Mini-courbe de tendance (sparkline) pour accompagner un KPI sans occuper d'espace.
 * Sans axes ni légende : uniquement la forme de la tendance.
 *
 * @param {Array} data
 * @param {string} dataKey
 * @param {string} [color]
 * @param {number} [height]
 */
export function WsSparkline({ data, dataKey = 'value', color = C.primary, height = 34 }) {
  if (!data || data.length < 2) return null
  const id = `spark-${dataKey}`
  return (
    <div className="ws-sparkline" style={{ height }}>
      <ResponsiveContainer width="100%" height={height}>
        <AreaChart data={data} margin={{ top: 2, right: 0, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={color} stopOpacity={0.3} />
              <stop offset="100%" stopColor={color} stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <Area type="monotone" dataKey={dataKey} stroke={color} strokeWidth={1.6} fill={`url(#${id})`} dot={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

/* Triplet RGB du teal principal, nécessaire pour composer des intensités (heatmap). */
/**
 * Nuage de points pour comparer une consommation mesuree avec un facteur meteo
 * comme ET0, temperature ou pluie.
 */
export function WsScatterChart({
  data = [],
  xKey,
  yKey,
  name = 'Mesures',
  xUnit = 'mm',
  yUnit = 'litres',
  height = 280,
  color = C.primary,
  exportName,
}) {
  return (
    <ChartShell
      unit={`${yUnit} selon ${xUnit}`}
      exportName={exportName}
      exportRows={data}
      exportColumns={[{ key: xKey, label: xUnit }, { key: yKey, label: yUnit }]}
    >
      <ResponsiveContainer width="100%" height={height}>
        <ScatterChart margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={C.line} />
          <XAxis
            type="number"
            dataKey={xKey}
            name={xUnit}
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            label={{ value: xUnit, position: 'insideBottomRight', offset: -2, fill: C.muted, fontSize: 10 }}
          />
          <YAxis
            type="number"
            dataKey={yKey}
            name={yUnit}
            tick={axisTick}
            axisLine={false}
            tickLine={false}
            width={52}
            label={{ value: yUnit, angle: -90, position: 'insideLeft', fill: C.muted, fontSize: 10 }}
          />
          <ZAxis range={[48, 48]} />
          <Tooltip
            cursor={{ strokeDasharray: '3 3' }}
            contentStyle={tooltipStyle}
            formatter={(value, key) => [Number(value).toLocaleString('fr-FR'), key === yKey ? yUnit : xUnit]}
          />
          <Scatter name={name} data={data} fill={color} />
        </ScatterChart>
      </ResponsiveContainer>
    </ChartShell>
  )
}

const primaryRgb = '10,130,118'

/**
 * Jauge horizontale à seuils : la valeur est située dans des zones colorées
 * (critique / faible / normal). Un simple pourcentage ne dit pas si la valeur est
 * préoccupante ; cette jauge le rend immédiatement lisible.
 *
 * @param {number} value
 * @param {number} [max]
 * @param {Array} thresholds - [{ upTo, color, label }] zones croissantes, la dernière couvrant le max.
 * @param {string} [unit]
 * @param {string} [caption]
 */
export function WsThresholdGauge({ value = 0, max = 100, thresholds = [], unit = '%', caption }) {
  const safeMax = max || 100
  const pct = Math.max(0, Math.min(100, (Number(value) / safeMax) * 100))
  const zones = (thresholds || []).reduce((acc, zone) => {
    const from = acc.cursor
    const to = Math.min(safeMax, zone.upTo)
    acc.items.push({ ...zone, from, to, width: Math.max(0, ((to - from) / safeMax) * 100) })
    acc.cursor = to
    return acc
  }, { cursor: 0, items: [] }).items

  return (
    <div className="ws-threshold-gauge">
      <div className="ws-threshold-gauge-track" role="img" aria-label={`Valeur ${Math.round(pct)}%`}>
        {zones.map((zone) => (
          <span key={zone.label || zone.upTo} className="ws-threshold-gauge-zone" style={{ width: `${zone.width}%`, background: zone.color }} />
        ))}
        <span className="ws-threshold-gauge-marker" style={{ left: `${pct}%` }} />
      </div>
      <div className="ws-threshold-gauge-value">
        {Math.round(Number(value) || 0)}
        {unit}
        {caption && <small>{caption}</small>}
      </div>
      {zones.length > 0 && (
        <ul className="ws-threshold-gauge-legend">
          {zones.map((zone) => (
            <li key={zone.label || zone.upTo}>
              <span className="ws-legend-swatch" style={{ background: zone.color }} />
              {zone.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/**
 * Graphique "bullet" : une valeur comparée à un objectif, dans un référentiel de zones.
 * Utilisé pour les quotas de consommation (objectif mensuel vs réel).
 *
 * @param {number} value
 * @param {number} target
 * @param {number} max
 * @param {Array} [zones] - [{ upTo, color }] zones de fond.
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
        <span className="ws-bullet-target" style={{ left: `${pct(target)}%` }} title={`Objectif ${formatLiters(target)}`} />
      </div>
      <div className="ws-bullet-caption">
        <strong>{formatLiters(value)}</strong>
        {` / objectif ${formatLiters(target)}${unit === '%' ? ' %' : ''}`}
      </div>
    </div>
  )
}

/**
 * Carte de chaleur : met en évidence les zones qui consomment le plus selon les jours.
 * Un tableau dense se lit mal ; l'intensité de couleur fait ressortir les surconsommations.
 *
 * @param {Array} rows - [{ id, name }] (ex. zones)
 * @param {Array} columns - [{ id, label }] (ex. jours)
 * @param {Function} valueOf - (rowId, columnId) => number | null
 * @param {'liters'|'percent'} [unitKind]
 */
export function WsHeatmap({ rows = [], columns = [], valueOf, unitKind = 'liters' }) {
  const values = []
  rows.forEach((row) => columns.forEach((col) => values.push(valueOf(row.id, col.id))))
  const max = values.reduce((acc, v) => Math.max(acc, Number(v) || 0), 0)
  const cellColor = (v) => {
    const ratio = max === 0 ? 0 : (Number(v) || 0) / max
    return `rgba(${primaryRgb},${(0.06 + ratio * 0.7).toFixed(2)})`
  }
  return (
    <div className="ws-heatmap-wrap">
      <ChartShell unit={unitKind === 'liters' ? 'litres (intensité de couleur)' : '%'}>
        <div className="ws-heatmap-scroll">
          <table className="ws-heatmap">
            <thead>
              <tr>
                <th />
                {columns.map((col) => (
                  <th key={col.id}>{col.label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <th scope="row">{row.name}</th>
                  {columns.map((col) => {
                    const v = valueOf(row.id, col.id)
                    return (
                      <td key={col.id} style={{ background: v ? cellColor(v) : undefined }} title={`${row.name} · ${col.label} : ${formatLiters(v)}`}>
                        {v ? Math.round(Number(v)) : ''}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ChartShell>
    </div>
  )
}
