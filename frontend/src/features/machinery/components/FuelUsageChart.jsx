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
import EmptyState from "../../../shared/components/EmptyState";

function formatTooltipValue(value, name) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return value;
  if (name === "Fuel cost ($)") return [`$${amount.toFixed(2)}`, name];
  return [`${amount} h`, name];
}

function FuelUsageChart({ data }) {
  const hasData = data.some((point) => point.fuelCost > 0 || point.usageHours > 0);

  if (!hasData) {
    return <EmptyState message="No fuel or usage recorded over the last 6 months." />;
  }

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#eef2f1" />
          <XAxis dataKey="month" tickLine={false} axisLine={false} tick={{ fill: "#94a3b8", fontSize: 12 }} />
          <YAxis
            yAxisId="cost"
            tickLine={false}
            axisLine={false}
            width={46}
            tick={{ fill: "#94a3b8", fontSize: 12 }}
            tickFormatter={(value) => `$${value}`}
          />
          <YAxis
            yAxisId="hours"
            orientation="right"
            tickLine={false}
            axisLine={false}
            width={40}
            tick={{ fill: "#94a3b8", fontSize: 12 }}
            tickFormatter={(value) => `${value}h`}
          />
          <Tooltip formatter={formatTooltipValue} contentStyle={{ borderRadius: 12, border: "1px solid #e5e7eb", fontSize: 12 }} />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar
            yAxisId="cost"
            dataKey="fuelCost"
            name="Fuel cost ($)"
            fill="#0a8276"
            radius={[6, 6, 0, 0]}
            maxBarSize={38}
            animationDuration={900}
          />
          <Line
            yAxisId="hours"
            type="monotone"
            dataKey="usageHours"
            name="Usage (hours)"
            stroke="#d9a441"
            strokeWidth={2.5}
            dot={{ r: 3, strokeWidth: 0, fill: "#d9a441" }}
            activeDot={{ r: 5 }}
            animationDuration={1100}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
export default FuelUsageChart;
