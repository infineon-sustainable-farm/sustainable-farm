import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useTranslation } from "react-i18next";
import { EmptyState } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

// Colours and sizes from the tokens of src/index.css (chart palette, text-xs, radius-tile). The
// axis ticks are SVG attributes: their font size is the text-xs value as a number.
const TEXT_XS = 12;
const AXIS_TICK = { fill: "var(--color-chart-3)", fontSize: TEXT_XS };
const TOOLTIP_STYLE = {
  borderRadius: "var(--radius-tile)",
  border: "thin solid var(--color-border-divider)",
  fontSize: "var(--text-xs)",
};
const LEGEND_STYLE = { fontSize: "var(--text-xs)" };

function FuelUsageChart({ data }) {
  const { t } = useTranslation("machinery");
  const format = useMachineryFormat();
  const hasData = data.some((point) => point.fuelCost > 0 || point.usageHours > 0);

  if (!hasData) {
    return <EmptyState message={t("dashboard.chartEmpty")} />;
  }

  function formatTooltipValue(value, name, item) {
    const amount = Number(value);
    if (!Number.isFinite(amount)) return value;
    if (item?.dataKey === "fuelCost") return [format.currency(amount), name];
    return [t("units.hours", { value: format.number(amount, 1) }), name];
  }

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
          <XAxis dataKey="month" tickLine={false} axisLine={false} tick={AXIS_TICK} />
          <YAxis
            yAxisId="cost"
            tickLine={false}
            axisLine={false}
            width={46}
            tick={AXIS_TICK}
            tickFormatter={(value) => format.currencyTick(value)}
          />
          <YAxis
            yAxisId="hours"
            orientation="right"
            tickLine={false}
            axisLine={false}
            width={40}
            tick={AXIS_TICK}
            tickFormatter={(value) => t("units.hoursTick", { value: format.number(value) })}
          />
          <Tooltip formatter={formatTooltipValue} contentStyle={TOOLTIP_STYLE} />
          <Legend wrapperStyle={LEGEND_STYLE} />
          <Bar
            yAxisId="cost"
            dataKey="fuelCost"
            name={t("dashboard.fuelCostSeries")}
            fill="var(--color-chart-1)"
            radius={[6, 6, 0, 0]}
            maxBarSize={38}
            isAnimationActive={false}
          />
          <Line
            yAxisId="hours"
            type="monotone"
            dataKey="usageHours"
            name={t("dashboard.usageSeries")}
            stroke="var(--color-chart-2)"
            strokeWidth={2.5}
            dot={{ r: 3, strokeWidth: 0, fill: "var(--color-chart-2)" }}
            activeDot={{ r: 5 }}
            isAnimationActive={false}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
export default FuelUsageChart;
