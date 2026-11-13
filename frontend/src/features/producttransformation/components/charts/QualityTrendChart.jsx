import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PTChartShell from '../PTChartShell';
import { QUALITY_TARGET_PERCENT, hasRows } from '../../utils/dashboardMetrics';
import {
  CHART_AXIS_TICK,
  CHART_GRID_STROKE,
  CHART_HEIGHT,
  CHART_LEGEND_STYLE,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'date', label: 'Date' },
  { key: 'passRate', label: 'Pass rate (%)' },
];

export default function QualityTrendChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Quality Pass Rate Trend"
      subtitle={`QC pass rate against the ${QUALITY_TARGET_PERCENT}% target`}
      unit="%"
      demo={demo}
      empty={!hasRows(data)}
      exportName="quality-trend"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="160ms"
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_STROKE} />
              <XAxis dataKey="date" tickLine={false} axisLine={false} tick={CHART_AXIS_TICK} />
              <YAxis
                domain={[0, 100]}
                tickLine={false}
                axisLine={false}
                width={40}
                tick={CHART_AXIS_TICK}
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Legend wrapperStyle={CHART_LEGEND_STYLE} />
              <ReferenceLine
                y={QUALITY_TARGET_PERCENT}
                stroke="#d9a441"
                strokeDasharray="5 4"
                label={{
                  value: `Target ${QUALITY_TARGET_PERCENT}%`,
                  position: 'insideBottomRight',
                  fill: '#8e661c',
                  fontSize: 11,
                }}
              />
              <Line
                type="monotone"
                dataKey="passRate"
                name="Pass rate (%)"
                stroke="#0a8276"
                strokeWidth={2.5}
                dot={{ r: 3, strokeWidth: 0, fill: '#0a8276' }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No quality trend data for this period.
        </div>
      )}
    </PTChartShell>
  );
}
