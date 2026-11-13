import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PTChartShell from '../PTChartShell';
import { hasRows } from '../../utils/dashboardMetrics';
import {
  CHART_AXIS_TICK,
  CHART_GRID_STROKE,
  CHART_HEIGHT,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'equipment', label: 'Equipment' },
  { key: 'utilization', label: 'Utilization (%)' },
];

export default function EquipmentUtilizationChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Equipment Utilization"
      subtitle="Per equipment (%)"
      unit="%"
      demo={demo}
      empty={!hasRows(data)}
      exportName="equipment-utilization"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="240ms"
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} layout="vertical" margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={CHART_GRID_STROKE} />
              <XAxis
                type="number"
                domain={[0, 100]}
                tickLine={false}
                axisLine={false}
                tick={CHART_AXIS_TICK}
                tickFormatter={(value) => `${value}%`}
              />
              <YAxis
                dataKey="equipment"
                type="category"
                width={128}
                tickLine={false}
                axisLine={false}
                tick={CHART_AXIS_TICK}
              />
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Bar
                dataKey="utilization"
                name="Utilization (%)"
                fill="#3a7ca5"
                radius={[0, 6, 6, 0]}
                maxBarSize={22}
              />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No equipment data available.
        </div>
      )}
    </PTChartShell>
  );
}
