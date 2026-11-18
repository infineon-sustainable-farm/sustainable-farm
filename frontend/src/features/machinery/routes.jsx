import { Boxes, ClipboardList, Cog, Fuel, LayoutDashboard, Tractor, Users, Wrench } from "lucide-react";
import Dashboard from "./components/Dashboard";
import EquipmentRegistry from "./components/EquipmentRegistry";
import OperatorAssignments from "./components/OperatorAssignments";
import SpareParts from "./components/SpareParts";
import MaintenanceSchedule from "./components/MaintenanceSchedule";
import UsageFuelLog from "./components/UsageFuelLog";
import RepairLog from "./components/RepairLog";
import LowStockNotifications from "./components/LowStockNotifications";

// The screens render inside the application shell (app/AppLayout.jsx), see nav.shell below.
export default [
  {
    path: "machinery",
    children: [
      { index: true, element: <Dashboard /> },
      { path: "equipment-registry", element: <EquipmentRegistry /> },
      { path: "operators", element: <OperatorAssignments /> },
      { path: "spare-parts", element: <SpareParts /> },
      { path: "maintenance", element: <MaintenanceSchedule /> },
      { path: "usage-fuel-log", element: <UsageFuelLog /> },
      { path: "repair-log", element: <RepairLog /> },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Machinery renders inside the shell; the low stock notifications
 * sit at the bottom of the sidebar while a machinery screen is open.
 */
export const nav = {
  id: "machinery",
  basePath: "/machinery",
  icon: Tractor,
  nameKey: "machinery:name",
  shell: true,
  sidebarFooter: LowStockNotifications,
  items: [
    { to: "/machinery", labelKey: "machinery:nav.dashboard", icon: LayoutDashboard, end: true },
    { to: "/machinery/equipment-registry", labelKey: "machinery:nav.equipmentRegistry", icon: Cog },
    { to: "/machinery/maintenance", labelKey: "machinery:nav.maintenance", icon: Wrench },
    { to: "/machinery/usage-fuel-log", labelKey: "machinery:nav.usageFuelLog", icon: Fuel },
    { to: "/machinery/repair-log", labelKey: "machinery:nav.repairLog", icon: ClipboardList },
    { to: "/machinery/operators", labelKey: "machinery:nav.operators", icon: Users },
    { to: "/machinery/spare-parts", labelKey: "machinery:nav.spareParts", icon: Boxes },
  ],
};
