import { lazy, Suspense, useMemo } from "react";
import { Boxes, CalendarClock, Clock, Cog, Fuel, TriangleAlert, Wrench } from "lucide-react";
import machineryBrand from "../../../assets/machineryBrand.webp";
import { useDashboardData } from "../hooks/useDashboardData";
import { buildDashboardMetrics } from "../dashboardMetrics";
import DashboardKpiCard from "./DashboardKpiCard";
import NeedsAttentionPanel from "./NeedsAttentionPanel";
import ErrorState from "../../../shared/components/ErrorState";
import { formatCurrency, formatDecimal } from "../../../shared/utils/formatDecimal";

const FuelUsageChart = lazy(() => import("./FuelUsageChart"));

function StatChip({ icon: Icon, value, label, tone = "text-primary" }) {
  return (
    <div className="flex items-center gap-2 rounded-xl bg-gray-50 px-3 py-2">
      <Icon size={16} className={tone} />
      <div className="leading-tight">
        <p className="text-sm font-semibold text-gray-800">{value}</p>
        <p className="text-[11px] text-gray-400">{label}</p>
      </div>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="p-6 space-y-6">
      <div className="h-8 w-52 rounded-lg bg-gray-100 animate-pulse" />
      <div className="h-44 rounded-2xl bg-gray-100 animate-pulse" />
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[0, 1, 2, 3].map((index) => (
          <div key={index} className="h-40 rounded-2xl bg-gray-100 animate-pulse" />
        ))}
      </div>
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <div className="xl:col-span-2 h-80 rounded-2xl bg-gray-100 animate-pulse" />
        <div className="h-80 rounded-2xl bg-gray-100 animate-pulse" />
      </div>
    </div>
  );
}

function Dashboard() {
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
      }),
    [equipments, spareParts, lowStockSpareParts, maintenanceSchedules, usageLogs, fuelLogs, repairLogs]
  );

  if (isLoading) return <DashboardSkeleton />;

  if (isError) {
    return (
      <div className="p-6">
        <ErrorState message="Failed to load the dashboard. Please try again." onRetry={refetchAll} />
      </div>
    );
  }

  const today = new Date().toLocaleDateString("en-US", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  function getEquipmentName(equipmentId) {
    if (equipmentId == null) return "—";
    return metrics.equipmentNames.get(equipmentId) ?? `#${equipmentId}`;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3 animate-fade-in">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-sm text-gray-500">
            Overview of assets, inventory, maintenance and field operations.
          </p>
        </div>
        <p className="text-sm text-gray-400">{today}</p>
      </div>

      <section className="animate-scale-in overflow-hidden rounded-2xl shadow-sm ring-1 ring-black/5">
        <img
          src={machineryBrand}
          alt="Know your fleet. Grow your farm."
          className="h-40 md:h-52 w-full object-cover object-center"
        />
      </section>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <DashboardKpiCard
          to="/machinery/equipment-registry"
          icon={Cog}
          label="Total assets"
          value={metrics.totalAssets}
          hint={`${metrics.operationalRate}% operational · ${metrics.underMaintenance} under maintenance · ${metrics.outOfService} out of service`}
          delay={0}
        />
        <DashboardKpiCard
          to="/machinery/maintenance"
          icon={CalendarClock}
          label="Maintenance overdue"
          value={metrics.overdueCount}
          hint={metrics.dueSoonCount > 0 ? `${metrics.dueSoonCount} due within 7 days` : "Nothing due in the next 7 days"}
          tone={metrics.overdueCount > 0 ? "error" : "primary"}
          delay={80}
        />
        <DashboardKpiCard
          to="/machinery/spare-parts"
          icon={TriangleAlert}
          label="Low stock parts"
          value={metrics.lowStockCount}
          hint="At or below their reorder threshold"
          tone="warning"
          delay={160}
        />
        <DashboardKpiCard
          to="/machinery/spare-parts"
          icon={Boxes}
          label="Inventory value"
          value={formatCurrency(metrics.inventoryValue)}
          hint={`${metrics.sparePartCount} spare parts tracked`}
          tone="info"
          delay={240}
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-5">
        <section
          className="animate-fade-up xl:col-span-2 bg-white rounded-2xl border border-gray-100 shadow-sm p-5"
          style={{ animationDelay: "320ms" }}
        >
          <div className="flex flex-wrap items-start justify-between gap-3 mb-4">
            <div>
              <h2 className="text-lg font-semibold">Fuel spend &amp; usage</h2>
              <p className="text-sm text-gray-500">Last 6 months of field operations</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <StatChip icon={Fuel} value={formatCurrency(metrics.totalFuelCost)} label={`${formatDecimal(metrics.totalFuelLiters, 1)} L`} />
              <StatChip icon={Clock} value={`${formatDecimal(metrics.totalUsageHours, 1)} h`} label="usage" tone="text-amber-600" />
              <StatChip icon={Wrench} value={formatCurrency(metrics.totalRepairCost)} label={`${formatDecimal(metrics.totalDowntime, 1)} h downtime`} tone="text-sky-700" />
            </div>
          </div>
          <Suspense fallback={<div className="h-72 w-full rounded-xl bg-gray-50 animate-pulse" />}>
            <FuelUsageChart data={metrics.chartData} />
          </Suspense>
        </section>

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
