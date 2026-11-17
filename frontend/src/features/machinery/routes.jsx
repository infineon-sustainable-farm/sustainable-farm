import { Boxes, ClipboardList, Cog, Fuel, LayoutDashboard, Tractor, Users, Wrench } from "lucide-react";
import MachineryLayout from "./components/MachineryLayout";
import Dashboard from "./components/Dashboard";
import EquipmentRegistry from "./components/EquipmentRegistry";
import OperatorAssignments from "./components/OperatorAssignments";
import SpareParts from "./components/SpareParts";
import MaintenanceSchedule from "./components/MaintenanceSchedule";
import UsageFuelLog from "./components/UsageFuelLog";
import RepairLog from "./components/RepairLog";

export default [
  {
    path: "machinery",
    element: <MachineryLayout />,
    children:[
      {index: true, element: <Dashboard/> },
      {path: "equipment-registry", element: <EquipmentRegistry/> },
      {path: "operators", element: <OperatorAssignments/> },
      {path: "spare-parts", element: <SpareParts/> },
      {path: "maintenance", element: <MaintenanceSchedule/> },
      {path: "usage-fuel-log", element: <UsageFuelLog/> },
      {path: "repair-log", element: <RepairLog/> },
    ],
  },
];

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Same entries, order and targets as the module's own sidebar.
 */
export const nav = {
  id: "machinery",
  basePath: "/machinery",
  icon: Tractor,
  nameKey: "machinery:name",
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
