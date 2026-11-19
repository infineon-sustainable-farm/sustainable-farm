import { useEffect, useState } from "react";
import { CloudSun, Gauge, Leaf, Zap } from "lucide-react";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useTranslation } from "react-i18next";
import { getComponents, getDashboardSummary, getSolarLast7Days } from "../api/energyApi";
import { AXIS_TICK, BAR_RADIUS, TOOLTIP_STYLE } from "../chartStyles";
import { statusTone, useEnergyFormat, useEnumLabels } from "../format";
import { Card, EmptyState, ErrorState, PageHeader, StatusBadge, Table } from "../../../shared/ui";
import EnergyKpiCard from "./EnergyKpiCard";

export default function Dashboard() {
  const { t } = useTranslation("energysupply");
  const format = useEnergyFormat();
  const labels = useEnumLabels();
  const [summary, setSummary] = useState(null);
  const [components, setComponents] = useState([]);
  const [weekly, setWeekly] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getDashboardSummary(), getComponents(), getSolarLast7Days()])
      .then(([summaryData, componentsData, solarLogs]) => {
        setSummary(summaryData);
        setComponents(componentsData);
        const totalsByDay = new Map();
        solarLogs.forEach((log) => {
          totalsByDay.set(
            log.date,
            (totalsByDay.get(log.date) ?? 0) + Number(log.kwhProduced ?? 0)
          );
        });
        setWeekly(
          [...totalsByDay]
            .sort(([dateA], [dateB]) => dateA.localeCompare(dateB))
            .map(([day, solar]) => ({ day, solar }))
        );
      })
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return (
      <div className="p-6">
        <ErrorState message={t("dashboard.loadError", { error })} />
      </div>
    );
  }

  if (!summary) {
    return <p role="status" className="p-6 text-sm text-text-muted">{t("dashboard.loading")}</p>;
  }

  const columns = [
    {
      key: "name",
      header: t("dashboard.component"),
      cell: (row) => (
        <>
          <div className="font-semibold">{row.name}</div>
          <div className="text-xs font-normal text-text-muted">{labels.category(row.category)}</div>
        </>
      ),
    },
    {
      key: "capacityValue",
      header: t("dashboard.value"),
      cell: (row) => (row.capacityValue == null ? t("dashboard.noValue") : format.number(row.capacityValue)),
    },
    {
      key: "status",
      header: t("fields.status"),
      cell: (row) => <StatusBadge tone={statusTone(row.status)} label={labels.status(row.status)} />,
    },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title={t("dashboard.title")} subtitle={t("dashboard.subtitle")} />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <EnergyKpiCard
          label={t("dashboard.totalPvCapacity")}
          value={format.number(summary.totalPvCapacityKwp ?? 0)}
          unit={t("units.kwp")}
          icon={CloudSun}
        />
        <EnergyKpiCard
          label={t("dashboard.dailyConsumption")}
          value={format.number(summary.currentDailyConsumptionKwh ?? 0)}
          unit={t("units.kwhPerDay")}
          icon={Gauge}
        />
        <EnergyKpiCard label={t("dashboard.generatorStatus")} value={labels.status(summary.generatorStatus)} icon={Zap} />
        <EnergyKpiCard
          label={t("dashboard.co2AvoidedMonth")}
          value={format.number(summary.co2AvoidedThisMonthKg ?? 0)}
          unit={t("units.kg")}
          icon={Leaf}
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-5">
        <Card className="xl:col-span-3" title={t("dashboard.solarChartTitle")}>
          {weekly.length === 0 ? (
            <EmptyState message={t("dashboard.solarChartEmpty")} />
          ) : (
            <div className="h-60 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={weekly}>
                  <XAxis dataKey="day" tick={AXIS_TICK} tickFormatter={(day) => format.date(day)} />
                  <YAxis tick={AXIS_TICK} tickFormatter={(value) => format.number(value)} />
                  <Tooltip
                    contentStyle={TOOLTIP_STYLE}
                    labelFormatter={(day) => format.date(day)}
                    formatter={(value) => [format.number(value), t("dashboard.solarSeries")]}
                  />
                  <Bar dataKey="solar" fill="var(--color-chart-1)" radius={BAR_RADIUS} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card className="xl:col-span-2" title={t("dashboard.statusTitle")}>
          <Table
            caption={t("dashboard.statusTitle")}
            columns={columns}
            rows={components}
            getRowKey={(row) => row.componentId}
            emptyMessage={t("components.empty")}
          />
        </Card>
      </div>
    </div>
  );
}
