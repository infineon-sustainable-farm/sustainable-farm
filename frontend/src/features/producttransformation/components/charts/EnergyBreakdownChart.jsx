import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import PTChartShell from '../PTChartShell';
import { hasRows } from '../../utils/dashboardMetrics';
import { CHART_HEIGHT, CHART_LEGEND_STYLE, CHART_TOOLTIP_STYLE } from './chartTheme';

const COLORS = ['#0a8276', '#d9a441', '#2e9e6b', '#3a7ca5', '#8e24aa', '#c1503f'];

const COLUMNS = [
  { key: 'name', label: 'Source' },
  { key: 'value', label: 'Consumption' },
];

export default function EnergyBreakdownChart({ data, demo = false }) {
  return (
    <PTChartShell
      title="Energy Consumption Breakdown"
      subtitle="Grid vs solar vs generator"
      unit="kWh"
      demo={demo}
      empty={!hasRows(data)}
      exportName="energy-breakdown"
      exportRows={data}
      exportColumns={COLUMNS}
      animationDelay="320ms"
    >
      {hasRows(data) ? (
        <div style={{ height: CHART_HEIGHT }} className="w-full">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={52}
                outerRadius={92}
                paddingAngle={2}
                dataKey="value"
                nameKey="name"
                label={({ name, percent }) => `${name} ${Math.round((percent ?? 0) * 100)}%`}
              >
                {data.map((entry, index) => (
                  <Cell
                    key={`cell-${entry.name}-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip contentStyle={CHART_TOOLTIP_STYLE} />
              <Legend wrapperStyle={CHART_LEGEND_STYLE} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div
          style={{ height: CHART_HEIGHT }}
          className="flex items-center justify-center text-sm text-gray-500"
        >
          No energy breakdown data available.
        </div>
      )}
    </PTChartShell>
  );
}
