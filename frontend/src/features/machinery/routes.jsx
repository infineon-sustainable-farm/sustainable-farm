import MachineryLayout from "./components/MachineryLayout";
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
      {path: "equipment-registry", element: <EquipmentRegistry/> },
      {path: "operators", element: <OperatorAssignments/> },
      {path: "spare-parts", element: <SpareParts/> },
      {path: "maintenance", element: <MaintenanceSchedule/> },
      {path: "usage-fuel-log", element: <UsageFuelLog/> },
      {path: "repair-log", element: <RepairLog/> },
    ],
  },
];
