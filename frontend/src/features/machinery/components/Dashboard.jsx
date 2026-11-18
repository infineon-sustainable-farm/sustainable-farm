import { lazy, Suspense, useMemo } from "react";
import { Boxes, CalendarClock, Clock, Cog, Fuel, TriangleAlert, Wrench } from "lucide-react";
import { useTranslation } from "react-i18next";
import machineryBrand from "../../../assets/machineryBrand.webp";
import { useDashboardData } from "../hooks/useDashboardData";
import { buildDashboardMetrics } from "../dashboardMetrics";
import { useMachineryFormat } from "../format";
import NeedsAttentionPanel from "./NeedsAttentionPanel";
import { Card, ErrorState, KpiCard, PageHeader, Skeleton } from "../../../shared/ui";

const FuelUsageChart = lazy(() => import("./FuelUsageChart"));

const CHIP_TONES = {
  primary: "text-primary",
  warning: "text-warning-fg",
  info: "text-info-fg",
};

function StatChip({ icon: Icon, value, label, tone = "primary" }) {
  return (
    <div className="flex items-center gap-2 rounded-tile bg-surface-muted px-3 py-2">
      <Icon size={16} aria-hidden="true" className={CHIP_TONES[tone]} />
      <div className="leading-tight">
        <p className="text-sm font-semibold text-text">{value}</p>
        <p className="text-xs text-text-muted">{label}</p>
      </div>
    </div>
  );
}

function DashboardSkeleton() {
  const { t } = useTranslation("common");
  return (
    <div className="p-6 space-y-6" aria-busy="true">
      <p role="status" className="sr-only">
        {t("status.loading")}
      </p>
      <Skeleton className="h-8 w-52" />
      <Skeleton radius="card" className="h-44" />
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((index) => (
          <Skeleton key={index} radius="card" className="h-40" />
        ))}
      </div>
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Skeleton radius="card" className="xl:col-span-2 h-80" />
        <Skeleton radius="card" className="h-80" />
      </div>
    </div>
  );
}

function Dashboard() {
  const { t } = useTranslation("machinery");
  const format = useMachineryFormat();
  const {
    isLoading,
    isError,
    refetchAll,
    equipments,
    spareParts,
    lowStockSpareParts,
    maintenanceSchedules,
    usageLogs,
    fuelLogs,
    repairLogs,
  } = useDashboardData();

  const metrics = useMemo(
    () =>
      buildDashboardMetrics({
        equipments,
        spareParts,
        lowStockSpareParts,
        maintenanceSchedules,
        usageLogs,
        fuelLogs,
        repairLogs,
        formatMonth: format.month,
      }),
    [equipments, spareParts, lowStockSpareParts, maintenanceSchedules, usageLogs, fuelLogs, repairLogs, format]
  );

  if (isLoading) return <DashboardSkeleton />;

  if (isError) {
    return (
      <div className="p-6">
        <ErrorState message={t("dashboard.loadError")} onRetry={refetchAll} retryLabel={t("common.tryAgain")} />
      </div>
    );
  }

  const today = format.longDate(new Date());

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return t("common.none");
    return metrics.equipmentNames.get(equipmentId) ?? t("common.unknownEquipment", { id: equipmentId });
  }

  return (
    <div className="p-6 space-y-6">
      <PageHeader title={t("dashboard.title")} subtitle={t("dashboard.subtitle")} meta={today} />

      <section className="overflow-hidden rounded-card border border-border-subtle shadow-raised">
        <img
          src={machineryBrand}
          alt={t("dashboard.bannerAlt")}
          className="h-40 md:h-52 w-full object-cover object-center"
        />
      </section>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard
          to="/machinery/equipment-registry"
          icon={Cog}
          label={t("dashboard.totalAssets")}
          value={metrics.totalAssets}
          hint={t("dashboard.totalAssetsHint", {
            rate: metrics.operationalRate,
            underMaintenance: metrics.underMaintenance,
            outOfService: metrics.outOfService,
          })}
        />
        <KpiCard
          to="/machinery/maintenance"
          icon={CalendarClock}
          label={t("dashboard.overdue")}
          value={metrics.overdueCount}
          hint={
            metrics.dueSoonCount > 0
              ? t("dashboard.dueSoonHint", { number: metrics.dueSoonCount })
              : t("dashboard.nothingDue")
          }
          tone={metrics.overdueCount > 0 ? "error" : "primary"}
        />
        <KpiCard
          to="/machinery/spare-parts"
          icon={TriangleAlert}
          label={t("dashboard.lowStock")}
          value={metrics.lowStockCount}
          hint={t("dashboard.lowStockHint")}
          tone="warning"
        />
        <KpiCard
          to="/machinery/spare-parts"
          icon={Boxes}
          label={t("dashboard.inventoryValue")}
          value={format.currency(metrics.inventoryValue)}
          hint={t("dashboard.inventoryHint", { number: metrics.sparePartCount })}
          tone="info"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <Card
          className="xl:col-span-2"
          title={t("dashboard.chartTitle")}
          subtitle={t("dashboard.chartSubtitle")}
          actions={
            <>
              <StatChip
                icon={Fuel}
                value={format.currency(metrics.totalFuelCost)}
                label={t("units.liters", { value: format.decimal(metrics.totalFuelLiters, 1) })}
              />
              <StatChip
                icon={Clock}
                value={t("units.hours", { value: format.decimal(metrics.totalUsageHours, 1) })}
                label={t("dashboard.usage")}
                tone="warning"
              />
              <StatChip
                icon={Wrench}
                value={format.currency(metrics.totalRepairCost)}
                label={t("dashboard.downtime", { value: format.decimal(metrics.totalDowntime, 1) })}
                tone="info"
              />
            </>
          }
        >
          <Suspense fallback={<Skeleton radius="tile" className="h-72 w-full" />}>
            <FuelUsageChart data={metrics.chartData} />
          </Suspense>
        </Card>

        <NeedsAttentionPanel
          overdueSchedules={metrics.overdueSchedules}
          lowStockList={metrics.lowStockList}
          getEquipmentName={getEquipmentName}
        />
      </div>
    </div>
  );
}
export default Dashboard;
