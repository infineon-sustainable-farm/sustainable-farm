const MS_PER_DAY = 86400000;

function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function daysUntil(dateString) {
  const date = parseIsoDate(dateString);
  if (!date) return null;
  return Math.round((date - startOfToday()) / MS_PER_DAY);
}

export function buildMonthlySeries(fuelLogs = [], usageLogs = [], monthCount = 6) {
  const now = new Date();
  const months = [];

  for (let index = monthCount - 1; index >= 0; index -= 1) {
    const date = new Date(now.getFullYear(), now.getMonth() - index, 1);
    months.push({
      key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      month: date.toLocaleDateString("en-US", { month: "short" }),
      fuelCost: 0,
      usageHours: 0,
    });
  }

  const byKey = new Map(months.map((entry) => [entry.key, entry]));

  fuelLogs.forEach((log) => {
    const month = byKey.get(log.date?.slice(0, 7));
    if (month) month.fuelCost += Number(log.cost) || 0;
  });

  usageLogs.forEach((log) => {
    const month = byKey.get(log.date?.slice(0, 7));
    if (month) month.usageHours += Number(log.hoursUsed) || 0;
  });

  return months.map(({ month, fuelCost, usageHours }) => ({
    month,
    fuelCost: Math.round(fuelCost * 100) / 100,
    usageHours: Math.round(usageHours * 10) / 10,
  }));
}

export function buildDashboardMetrics({
  equipments,
  spareParts,
  lowStockSpareParts,
  maintenanceSchedules,
  usageLogs,
  fuelLogs,
  repairLogs,
}) {
  const equipmentList = equipments?.content ?? [];
  const sparePartList = spareParts?.content ?? [];
  const scheduleList = maintenanceSchedules?.content ?? [];
  const usageList = usageLogs?.content ?? [];
  const fuelList = fuelLogs?.content ?? [];
  const repairList = repairLogs?.content ?? [];

  const totalAssets = equipments?.totalElements ?? equipmentList.length;
  const operational = equipmentList.filter((item) => item.status === "OPERATIONAL").length;
  const underMaintenance = equipmentList.filter((item) => item.status === "UNDER_MAINTENANCE").length;
  const outOfService = equipmentList.filter((item) => item.status === "OUT_OF_SERVICE").length;
  const operationalRate = equipmentList.length > 0 ? Math.round((operational / equipmentList.length) * 100) : 0;

  const lowStockCount = lowStockSpareParts?.length ?? 0;
  const inventoryValue = sparePartList.reduce(
    (sum, part) => sum + (Number(part.quantity) || 0) * (Number(part.unitCost) || 0),
    0
  );

  const withDueDays = scheduleList
    .map((schedule) => ({ ...schedule, dueInDays: daysUntil(schedule.nextDue) }))
    .filter((schedule) => schedule.dueInDays !== null);

  const overdueSchedules = withDueDays
    .filter((schedule) => schedule.dueInDays < 0)
    .sort((first, second) => first.dueInDays - second.dueInDays);

  const dueSoonSchedules = withDueDays
    .filter((schedule) => schedule.dueInDays >= 0 && schedule.dueInDays <= 7)
    .sort((first, second) => first.dueInDays - second.dueInDays);

  const totalUsageHours = usageList.reduce((sum, log) => sum + (Number(log.hoursUsed) || 0), 0);
  const totalFuelLiters = fuelList.reduce((sum, log) => sum + (Number(log.liters) || 0), 0);
  const totalFuelCost = fuelList.reduce((sum, log) => sum + (Number(log.cost) || 0), 0);
  const totalRepairCost = repairList.reduce((sum, log) => sum + (Number(log.cost) || 0), 0);
  const totalDowntime = repairList.reduce((sum, log) => sum + (Number(log.downtime) || 0), 0);

  const equipmentNames = new Map(equipmentList.map((equipment) => [equipment.id, equipment.name]));

  return {
    totalAssets,
    operational,
    underMaintenance,
    outOfService,
    operationalRate,
    lowStockCount,
    inventoryValue,
    sparePartCount: spareParts?.totalElements ?? sparePartList.length,
    overdueCount: overdueSchedules.length,
    dueSoonCount: dueSoonSchedules.length,
    totalUsageHours,
    totalFuelLiters,
    totalFuelCost,
    totalRepairCost,
    totalDowntime,
    overdueSchedules,
    dueSoonSchedules,
    lowStockList: (lowStockSpareParts ?? []).slice(0, 4),
    chartData: buildMonthlySeries(fuelList, usageList),
    equipmentNames,
  };
}
