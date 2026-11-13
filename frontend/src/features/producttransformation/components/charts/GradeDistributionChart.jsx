import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PTChartShell from '../PTChartShell';
import { hasRows } from '../../utils/dashboardMetrics';
import { GRADE_CHART_COLORS } from '../../styles/ptStyles';
import {
  CHART_AXIS_TICK,
  CHART_GRID_STROKE,
  CHART_LEGEND_STYLE,
  CHART_TOOLTIP_STYLE,
} from './chartTheme';

const COLUMNS = [
  { key: 'grade', label: 'Grade' },
  { key: 'quantityKg', label: 'Received quantity (kg)' },
  { key: 'count', label: 'Receptions' },
];

const colorOf = (grade) => GRADE_CHART_COLORS[grade] ?? '#6b7280';
const formatKg = (value) => `${Number(value).toFixed(2)} kg`;

/**
 * Grade split of the received mangoes. The pie answers "what share of the
 * volume is Grade A / B / C / D"; the bar answers "how many kilograms of
 * each". Both read the same aggregated rows.
 *
 * PNG export is disabled on this card because it renders two SVGs and the PNG
 * helper can only capture the first one honestly; CSV carries both figures.
 */
export default function GradeDistributionChart({ data, demo = false }) {
  const rows = hasRows(data) ? data : [];
  const pieRows = rows.filter((row) => Number(row.quantityKg) > 0);

  return (
    <PTChartShell
      title="Grade Distribution"
      subtitle="Received quantity by grade (kg)"
      unit="kg"
      demo={demo}
      empty={rows.length === 0}
      exportName="grade-distribution"
      exportRows={rows}
      exportColumns={COLUMNS}
      allowPng={false}
      animationDelay="120ms"
      note="Grades come from the raw-intake records; the letter is always written beside its colour."
    >
      {rows.length === 0 ? (
        <div className="flex h-72 items-center justify-center text-sm text-gray-500">
          No grade distribution data for this period.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieRows}
                  cx="50%"
                  cy="50%"
                  innerRadius={44}
                  outerRadius={78}
                  paddingAngle={2}
                  dataKey="quantityKg"
                  nameKey="grade"
                  label={({ grade, percent }) =>
                    `${grade} ${Math.round((percent ?? 0) * 100)}%`
                  }
                >
                  {pieRows.map((row) => (
                    <Cell key={`grade-pie-${row.grade}`} fill={colorOf(row.grade)} />
                  ))}
                </Pie>
                <Tooltip contentStyle={CHART_TOOLTIP_STYLE} formatter={formatKg} />
                <Legend wrapperStyle={CHART_LEGEND_STYLE} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART_GRID_STROKE} />
                <XAxis dataKey="grade" tickLine={false} axisLine={false} tick={CHART_AXIS_TICK} />
                <YAxis tickLine={false} axisLine={false} width={44} tick={CHART_AXIS_TICK} />
                <Tooltip contentStyle={CHART_TOOLTIP_STYLE} formatter={formatKg} />
                <Bar dataKey="quantityKg" name="Received (kg)" radius={[6, 6, 0, 0]} maxBarSize={38}>
                  {rows.map((row) => (
                    <Cell key={`grade-bar-${row.grade}`} fill={colorOf(row.grade)} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </PTChartShell>
  );
}
