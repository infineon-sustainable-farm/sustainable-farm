import { useEffect, useState } from "react";
import { CalendarRange, Leaf } from "lucide-react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useTranslation } from "react-i18next";
import { getCarbonMetrics } from "../api/energyApi";
import { AXIS_TICK, GRID_STROKE, TOOLTIP_STYLE } from "../chartStyles";
import { useEnergyFormat } from "../format";
import { Card, EmptyState, ErrorState, PageHeader, Table } from "../../../shared/ui";
import EnergyKpiCard from "./EnergyKpiCard";

export default function CarbonFootprint() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getCarbonMetrics()
      .then((data) => setMetrics([...data].sort((a, b) => a.period.localeCompare(b.period))))
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return (
      <div className="p-6">
        <ErrorState message={t("carbon.loadError", { error })} />
      </div>
    );
  }
  if (!metrics) return <p role="status" className="p-6 text-sm text-text-muted">{t("carbon.loading")}</p>;

  const latest = metrics[metrics.length - 1] || null;
  const currentYear = new Date().getFullYear().toString();
  const ytd = metrics
    .filter((m) => m.period.startsWith(currentYear))
    .reduce((s, m) => s + (m.co2AvoidedKg || 0), 0);

  const chartData = metrics.map((metric, index) => ({
    period: metric.period,
    cumulativeKg: metrics
      .slice(0, index + 1)
      .reduce((total, item) => total + (item.co2AvoidedKg || 0), 0),
  }));

  const columns = [
    {
      key: "period",
      header: t("carbon.period"),
      className: "whitespace-nowrap",
      cell: (row) => <span className="font-normal">{format.date(row.period)}</span>,
    },
    { key: "scenario", header: t("carbon.scenario") },
    {
      key: "co2AvoidedKg",
      header: t("carbon.co2AvoidedKg"),
      className: "font-medium tabular-nums",
      cell: (row) => format.number(row.co2AvoidedKg),
    },
    {
      key: "emissionFactorUsed",
      header: t("carbon.emissionFactor"),
      className: "tabular-nums",
      cell: (row) => format.number(row.emissionFactorUsed),
    },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("carbon.title")} subtitle={t("carbon.subtitle")} />

      <div className="grid gap-4 sm:grid-cols-3">
        <EnergyKpiCard
          label={t("carbon.latestPeriod")}
          value={latest?.co2AvoidedKg == null ? t("common.none") : format.number(latest.co2AvoidedKg)}
          unit={t("units.kg")}
          icon={Leaf}
        />
        <EnergyKpiCard label={t("carbon.ytd")} value={format.number(ytd)} unit={t("units.kg")} icon={CalendarRange} />
        <EnergyKpiCard label={t("carbon.latestScenario")} value={latest?.scenario ?? t("common.none")} icon={Leaf} />
      </div>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3" title={t("carbon.chartTitle")}>
          {chartData.length === 0 ? (
            <EmptyState message={t("carbon.empty")} />
          ) : (
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid stroke={GRID_STROKE} vertical={false} />
                  <XAxis dataKey="period" tick={AXIS_TICK} tickFormatter={(period) => format.date(period)} />
                  <YAxis tick={AXIS_TICK} tickFormatter={(value) => format.number(value)} />
                  <Tooltip
                    contentStyle={TOOLTIP_STYLE}
                    labelFormatter={(period) => format.date(period)}
                    formatter={(value) => [t("units.kgValue", { value: format.number(value) }), t("carbon.cumulativeSeries")]}
                  />
                  <Line
                    type="monotone"
                    dataKey="cumulativeKg"
                    stroke="var(--color-chart-1)"
                    strokeWidth={2.5}
                    dot={{ r: 3.5, fill: "var(--color-chart-1)" }}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>
        <Card className="xl:col-span-2" title={t("carbon.tableTitle")}>
          <Table
            caption={t("carbon.tableTitle")}
            columns={columns}
            rows={metrics}
            getRowKey={(row) => row.metricId}
            emptyMessage={t("carbon.empty")}
          />
        </Card>
      </div>
    </div>
  );
}
