/*
 * Machinery endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
export const MACHINERY_ENDPOINTS = {
  EQUIPMENTS: "/api/equipments",
  OPERATOR_ASSIGNMENTS: "/api/operator-assignments",
  SPARE_PARTS: "/api/machinery/spare-parts",
  MAINTENANCE_SCHEDULES: "/api/machinery/maintenance-schedules",
  USAGE_LOGS: "/api/machinery/usage-logs",
  FUEL_LOGS: "/api/machinery/fuel-logs",
  REPAIR_LOGS: "/api/machinery/repair-logs",
};
