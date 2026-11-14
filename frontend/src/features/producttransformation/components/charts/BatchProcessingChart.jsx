import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PTChartShell from '../PTChartShell';
import { totalBatches } from '../../utils/dashboardMetrics';
import { STATUS_CHART_COLORS } from '../../styles/ptStyles';
import { formatEnumLabel } from '../../../../shared/utils/formatEnumLabel';
import {
  CHART_AXIS_TICK,
  CHART_GRID_STROKE,
  CHART_HEIGHT,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'status', label: 'Status' },
  { key: 'count', label: 'Batches' },
];

const colorOf = (status) => STATUS_CHART_COLORS[status] ?? '#6b7280';

export default function BatchProcessingChart({ data, demo = false }) {
  const total = totalBatches(data);
  const hasBatches = total > 0;

  return (
    <PTChartShell
      title="Batch Processing Status"
      subtitle="Batches per processing stage"
      unit="batches"
      demo={demo}
      empty={!hasBatches}
      exportName="batch-processing-status"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="160ms"
      note={
        hasBatches
          ? `${total} batches in the pipeline. Stages with no batch are kept so the line reads end to end.`
          : undefined
      }
    >
      {hasBatches ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} layout="vertical" margin={{ top: 8, right: 16, bottom: 0, left: 8 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={CHART_GRID_STROKE} />
              <XAxis type="number" allowDecimals={false} tickLine={false} axisLine={false} tick={CHART_AXIS_TICK} />
              <YAxis
                dataKey="status"
                type="category"
                width={128}
                tickLine={false}
                axisLine={false}
                tick={CHART_AXIS_TICK}
                tickFormatter={(value) => formatEnumLabel(value)}
              />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Bar dataKey="count" name="Batches" radius={[0, 6, 6, 0]} maxBarSize={22}>
                {data.map((row) => (
                  <Cell key={`batch-status-${row.status}`} fill={colorOf(row.status)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No batch status data for this period.
        </div>
      )}
    </PTChartShell>
  );
}
